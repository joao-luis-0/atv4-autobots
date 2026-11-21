package com.autobots.automanager.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.autobots.automanager.entidades.Credencial;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;

@Component
public class JwtServico {
    public static final String EMISSOR = "autobots";
    private final SecretKey chave;
    private final long duracao;

    public JwtServico(@Value("${autobots.jwt.segredo:}") String segredo,
                      @Value("${autobots.jwt.duracao-segundos:3600}") long duracao) {
        byte[] bytes;
        if (segredo.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET deve ter pelo menos 32 bytes");
        }
        if (duracao < 1) throw new IllegalArgumentException("Duracao JWT deve ser positiva");
        chave = new SecretKeySpec(bytes, "HmacSHA256");
        this.duracao = duracao;
    }
    public SecretKey chave() { return chave; }
    public long duracao() { return duracao; }
    // Vincula o token a senha atual; trocar a senha invalida tokens anteriores.
    public String versao(Credencial credencial) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(credencial.getSenhaHash().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    public String emitir(Credencial credencial) {
        Instant agora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(EMISSOR)
            .subject(credencial.getId().toString()).issueTime(Date.from(agora))
            .expirationTime(Date.from(agora.plusSeconds(duracao)))
            .claim("versao", versao(credencial)).build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(chave.getEncoded()));
            return jwt.serialize();
        } catch (JOSEException ex) { throw new IllegalStateException("Falha ao emitir JWT", ex); }
    }
}
