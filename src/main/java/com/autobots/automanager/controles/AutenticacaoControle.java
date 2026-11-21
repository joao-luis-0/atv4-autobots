package com.autobots.automanager.controles;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.seguranca.*;

@RestController
@RequestMapping("/auth")
public class AutenticacaoControle {
    private final CredencialRepositorio credenciais;
    private final SenhaServico senhas;
    private final JwtServico jwt;
    public AutenticacaoControle(CredencialRepositorio credenciais, SenhaServico senhas, JwtServico jwt) {
        this.credenciais = credenciais; this.senhas = senhas; this.jwt = jwt;
    }
    public static class Login {
        public String login;
        public String senha;
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login entrada) {
        if (entrada.login == null || entrada.login.isBlank() || entrada.senha == null || entrada.senha.isBlank())
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe login e senha"));
        Credencial credencial = credenciais.findByLogin(entrada.login).orElse(null);
        if (credencial == null || !senhas.conferir(entrada.senha, credencial.getSenhaHash()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Login ou senha invalidos"));
        return ResponseEntity.ok(Map.of("token", jwt.emitir(credencial), "tipo", "Bearer", "expiraEm", jwt.duracao(),
            "usuarioId", credencial.getUsuario().getId(), "perfil", credencial.getUsuario().getPerfil()));
    }
}
