package com.autobots.automanager.seguranca;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SenhaServico {
    private static final int ITERACOES = 120_000;
    private static final int BITS = 256;
    private final SecureRandom random = new SecureRandom();

    public boolean conferir(String senha, String armazenada) {
        if (senha == null || armazenada == null) return false;
        try {
            String[] partes = armazenada.split("\\$");
            if (partes.length != 4 || !"pbkdf2_sha256".equals(partes[0])) return false;
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < 1 || iteracoes > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (esperado.length != BITS / 8) return false;
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, BITS);
            try {
                byte[] atual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                return java.security.MessageDigest.isEqual(esperado, atual);
            } finally { spec.clearPassword(); }
        } catch (Exception ex) { return false; }
    }

    public String gerarHash(String senha) {
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("A senha nao pode ser vazia");
        }
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, ITERACOES, BITS);
            try {
                byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                return "pbkdf2_sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
            } finally {
                spec.clearPassword();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar o hash da senha", ex);
        }
    }
}
