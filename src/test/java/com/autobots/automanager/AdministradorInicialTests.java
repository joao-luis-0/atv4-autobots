package com.autobots.automanager;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.autobots.automanager.entidades.PerfilUsuario;
import com.autobots.automanager.repositorios.*;
import com.autobots.automanager.seguranca.AdministradorInicial;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:bootstrap-test", "autobots.admin.login=primeiro-admin",
    "autobots.admin.senha=Inicial-estudo123!"})
@AutoConfigureMockMvc
@Transactional
class AdministradorInicialTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired CredencialRepositorio credenciais;
    @Autowired AdministradorInicial inicial;

    @Test void primeiroAdministradorConsegueLogin() throws Exception {
        assertEquals(1, usuarios.count());
        assertEquals(PerfilUsuario.ADMINISTRADOR, usuarios.findAll().get(0).getPerfil());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"primeiro-admin\",\"senha\":\"Inicial-estudo123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
    }
    @Test void bootstrapNaoDuplicaNemSobrescreveContaExistente() {
        var conta = credenciais.findByLogin("primeiro-admin").orElseThrow();
        String hash = conta.getSenhaHash();
        inicial.run(); inicial.run();
        assertEquals(1, usuarios.count()); assertEquals(1, credenciais.count());
        assertEquals(hash, credenciais.findByLogin("primeiro-admin").orElseThrow().getSenhaHash());
    }
}
