package com.autobots.automanager.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.repositorios.*;

/** Cria o primeiro administrador somente quando informado por variaveis de ambiente. */
@Component
public class AdministradorInicial implements CommandLineRunner {
    private final UsuarioRepositorio usuarios;
    private final CredencialRepositorio credenciais;
    private final SenhaServico senhas;
    @Value("${autobots.admin.login:}") private String login;
    @Value("${autobots.admin.senha:}") private String senha;
    public AdministradorInicial(UsuarioRepositorio usuarios, CredencialRepositorio credenciais, SenhaServico senhas) {
        this.usuarios = usuarios; this.credenciais = credenciais; this.senhas = senhas;
    }
    @Override @Transactional
    public void run(String... args) {
        if (login.isBlank() && senha.isBlank()) return;
        if (login.isBlank() || senha.isBlank()) throw new IllegalArgumentException("Informe ADMIN_LOGIN e ADMIN_PASSWORD juntos");
        // Nao sobrescreve dados nem reabre bootstrap apos existir qualquer conta.
        if (credenciais.count() > 0 || usuarios.count() > 0) return;
        Usuario admin = new Usuario();
        admin.setNome("Administrador inicial"); admin.setTipo(TipoUsuario.FUNCIONARIO);
        admin.setPerfil(PerfilUsuario.ADMINISTRADOR);
        usuarios.save(admin);
        Credencial credencial = new Credencial();
        credencial.setLogin(login); credencial.setSenhaHash(senhas.gerarHash(senha)); credencial.setUsuario(admin);
        credenciais.save(credencial);
    }
}
