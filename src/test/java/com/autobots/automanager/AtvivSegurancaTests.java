package com.autobots.automanager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.repositorios.*;
import com.autobots.automanager.seguranca.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AtvivSegurancaTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired CredencialRepositorio credenciais;
    @Autowired VendaRepositorio vendas;
    @Autowired ItemVendaRepositorio itens;
    @Autowired SenhaServico senhas;
    @Autowired JwtServico jwt;
    Map<String, Usuario> pessoas;
    Map<String, Credencial> contas;
    Map<String, String> tokens;
    Venda propria, alheia;

    @BeforeEach
    void preparar() throws Exception {
        pessoas = new HashMap<>(); contas = new HashMap<>(); tokens = new HashMap<>();
        String hash = senhas.gerarHash("Senha-teste123!");
        String[] logins = {"admin", "gerente", "vendedor", "outrovendedor", "cliente", "outrocliente"};
        PerfilUsuario[] perfis = {PerfilUsuario.ADMINISTRADOR, PerfilUsuario.GERENTE, PerfilUsuario.VENDEDOR,
            PerfilUsuario.VENDEDOR, PerfilUsuario.CLIENTE, PerfilUsuario.CLIENTE};
        for (int i = 0; i < logins.length; i++) {
            Usuario u = new Usuario(); u.setNome(logins[i]); u.setPerfil(perfis[i]); u.setTipo(TipoUsuario.CLIENTE);
            pessoas.put(logins[i], usuarios.save(u));
            Credencial c = new Credencial(); c.setUsuario(u); c.setLogin(logins[i]); c.setSenhaHash(hash);
            contas.put(logins[i], credenciais.save(c));
        }
        propria = venda("cliente", "vendedor"); alheia = venda("outrocliente", "outrovendedor");
    }
    Venda venda(String cliente, String vendedor) {
        Venda v = new Venda(); v.setCliente(pessoas.get(cliente)); v.setVendedor(pessoas.get(vendedor));
        return vendas.save(v);
    }
    String token(String login) throws Exception {
        if (!tokens.containsKey(login)) {
            String resposta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("login", login, "senha", "Senha-teste123!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
            tokens.put(login, mapper.readTree(resposta).get("token").asText());
        }
        return "Bearer " + tokens.get(login);
    }
    @Test void exigeAutenticacaoERetornaErroJson() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.erro").exists());
        mvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin\",\"senha\":\"incorreta\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }
    @ParameterizedTest
    @CsvSource({"admin,ADMINISTRADOR,201", "gerente,ADMINISTRADOR,403", "gerente,GERENTE,201",
        "gerente,VENDEDOR,201", "gerente,CLIENTE,201", "vendedor,ADMINISTRADOR,403",
        "vendedor,GERENTE,403", "vendedor,VENDEDOR,403", "vendedor,CLIENTE,201", "cliente,CLIENTE,403"})
    void criacaoRespeitaHierarquia(String login, String perfil, int status) throws Exception {
        mvc.perform(post("/api/usuarios").header("Authorization", token(login)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Novo\",\"perfil\":\"" + perfil + "\"}"))
            .andExpect(status().is(status));
    }
    @Test void gerenteNaoRebaixaAdminNemVendedorPromoveCliente() throws Exception {
        mvc.perform(put("/api/usuarios/" + pessoas.get("admin").getId()).header("Authorization", token("gerente"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"CLIENTE\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + pessoas.get("cliente").getId()).header("Authorization", token("vendedor"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"ADMINISTRADOR\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + pessoas.get("admin").getId()).header("Authorization", token("gerente")))
            .andExpect(status().isForbidden());
    }
    @Test void clienteSoLeProprioCadastro() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", token("cliente")))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(pessoas.get("cliente").getId()));
        mvc.perform(get("/api/usuarios/" + pessoas.get("outrocliente").getId()).header("Authorization", token("cliente")))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + pessoas.get("cliente").getId()).header("Authorization", token("cliente")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/clientes").header("Authorization", token("cliente"))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @CsvSource({"admin,2", "gerente,2", "vendedor,1", "cliente,1"})
    void listaVendasConformeVinculo(String login, int total) throws Exception {
        mvc.perform(get("/api/vendas").header("Authorization", token(login)))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(total)));
    }
    @Test void impedeAcessoPorIdAVendaEItensAlheios() throws Exception {
        ItemVenda item = new ItemVenda(); item.setVenda(alheia); itens.save(item);
        for (String login : List.of("vendedor", "cliente")) {
            mvc.perform(get("/api/vendas/" + alheia.getId()).header("Authorization", token(login)))
                .andExpect(status().isForbidden());
            mvc.perform(get("/api/itemvendas/" + item.getId()).header("Authorization", token(login)))
                .andExpect(status().isForbidden());
            mvc.perform(get("/api/itemvendas").header("Authorization", token(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(get("/api/vendas/" + propria.getId()).header("Authorization", token(login)))
                .andExpect(status().isOk());
        }
    }
    @Test void vendedorCriaApenasVendasPropriasENaoAlteraNemExclui() throws Exception {
        mvc.perform(post("/api/vendas").header("Authorization", token("vendedor")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"cliente\":{\"id\":" + pessoas.get("cliente").getId() + "}}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.vendedor.id").value(pessoas.get("vendedor").getId()));
        mvc.perform(post("/api/vendas").header("Authorization", token("vendedor")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"vendedor\":{\"id\":" + pessoas.get("outrovendedor").getId() + "}}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/vendas/" + propria.getId()).header("Authorization", token("vendedor"))
            .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());
        mvc.perform(delete("/api/vendas/" + propria.getId()).header("Authorization", token("vendedor")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/vendas").header("Authorization", token("cliente"))
            .contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());
    }
    @Test void vendedorAdicionaItensSomenteNaPropriaVenda() throws Exception {
        mvc.perform(post("/api/itemvendas").header("Authorization", token("vendedor")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"venda\":{\"id\":" + propria.getId() + "},\"quantidade\":1}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/itemvendas").header("Authorization", token("vendedor")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"venda\":{\"id\":" + alheia.getId() + "}}"))
            .andExpect(status().isForbidden());
    }
    @ParameterizedTest @CsvSource({"admin,201", "gerente,201", "vendedor,403", "cliente,403"})
    void escritaCatalogo(String login, int esperado) throws Exception {
        for (String rota : List.of("/api/servicos", "/api/mercadorias")) {
            mvc.perform(post(rota).header("Authorization", token(login)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\"}")) .andExpect(status().is(esperado));
            mvc.perform(get(rota).header("Authorization", token(login)))
                .andExpect(status().is(login.equals("cliente") ? 403 : 200));
        }
    }
    @Test void protegeCredenciaisContraEscaladaETrocaDeDono() throws Exception {
        mvc.perform(put("/api/credencials/" + contas.get("admin").getId()).header("Authorization", token("gerente"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"roubo\",\"senhaHash\":\"nova\",\"usuario\":{\"id\":"
                + pessoas.get("cliente").getId() + "}}")) .andExpect(status().isForbidden());
        mvc.perform(post("/api/credencials").header("Authorization", token("vendedor"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"roubo\",\"senhaHash\":\"nova\",\"usuario\":{\"id\":"
                + pessoas.get("admin").getId() + "}}")) .andExpect(status().isForbidden());
        mvc.perform(get("/api/credencials/" + contas.get("admin").getId()).header("Authorization", token("cliente")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/credencials").header("Authorization", token("gerente")))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
            .andExpect(content().string(not(containsString("pbkdf2_sha256"))));
    }
    @Test void tokenDeixaDeFuncionarAposTrocarSenhaOuExcluirCredencial() throws Exception {
        String antigo = token("cliente");
        Credencial c = contas.get("cliente"); c.setSenhaHash(senhas.gerarHash("OutraSenha!")); credenciais.saveAndFlush(c);
        mvc.perform(get("/api/usuarios").header("Authorization", antigo)).andExpect(status().isUnauthorized());
        String removido = token("vendedor"); credenciais.delete(contas.get("vendedor")); credenciais.flush();
        mvc.perform(get("/api/usuarios").header("Authorization", removido)).andExpect(status().isUnauthorized());
    }
    @Test void alteracaoDePerfilTemEfeitoNoTokenExistente() throws Exception {
        String autorizado = token("gerente");
        Usuario u = pessoas.get("gerente"); u.setPerfil(PerfilUsuario.CLIENTE); usuarios.saveAndFlush(u);
        mvc.perform(post("/api/servicos").header("Authorization", autorizado).contentType(MediaType.APPLICATION_JSON)
            .content("{}")) .andExpect(status().isForbidden());
    }
    @Test void rejeitaTokenExpiradoAdulteradoESemExpiracao() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer invalido")) .andExpect(status().isUnauthorized());
        Credencial c = contas.get("admin");
        for (boolean expirado : List.of(true, false)) {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(JwtServico.EMISSOR)
                .subject(c.getId().toString()).issueTime(Date.from(Instant.now().minusSeconds(120)))
                .claim("versao", jwt.versao(c));
            if (expirado) claims.expirationTime(Date.from(Instant.now().minusSeconds(60)));
            SignedJWT assinado = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            assinado.sign(new MACSigner(jwt.chave().getEncoded()));
            mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + assinado.serialize()))
                .andExpect(status().isUnauthorized());
        }
        String valido = token("admin");
        String[] partes = valido.substring(7).split("\\.");
        partes[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\"}".getBytes());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + String.join(".", partes)))
            .andExpect(status().isUnauthorized());
    }
    @Test void administradorRemoveAdministradorERevogaSeuToken() throws Exception {
        String antigo = token("admin");
        mvc.perform(delete("/api/usuarios/" + pessoas.get("admin").getId()).header("Authorization", antigo))
            .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(credenciais.findByLogin("admin").isPresent());
        mvc.perform(get("/api/usuarios").header("Authorization", antigo)).andExpect(status().isUnauthorized());
    }
    @Test void gerenteRemoveVendedorComCredencial() throws Exception {
        vendas.delete(alheia); vendas.flush();
        mvc.perform(delete("/api/usuarios/" + pessoas.get("outrovendedor").getId()).header("Authorization", token("gerente")))
            .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(credenciais.findByLogin("outrovendedor").isPresent());
    }
    @Test void loginDuplicadoEntraEmConflito() throws Exception {
        mvc.perform(post("/api/credencials").header("Authorization", token("admin"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"admin\",\"senhaHash\":\"nova\",\"usuario\":{\"id\":"
                + pessoas.get("cliente").getId() + "}}")) .andExpect(status().isConflict());
    }
}
