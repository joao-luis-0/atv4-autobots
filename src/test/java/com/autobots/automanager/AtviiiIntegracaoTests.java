package com.autobots.automanager;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMINISTRADOR")
class AtviiiIntegracaoTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private long criar(String rota, String json) throws Exception {
        MvcResult resposta = mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$._links.self.href").exists())
            .andReturn();
        JsonNode body = mapper.readTree(resposta.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void atualizar(String rota, long id, String json) throws Exception {
        mvc.perform(put(rota + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json))
           .andExpect(status().isOk()).andExpect(jsonPath("$._links.self.href").exists());
    }

    private void conferir(String rota, long id) throws Exception {
        mvc.perform(get(rota + "/" + id)).andExpect(status().isOk())
           .andExpect(jsonPath("$._links.self.href").exists())
           .andExpect(jsonPath("$._links.lista.href").exists());
        mvc.perform(get(rota)).andExpect(status().isOk());
    }

    private void excluir(String rota, long id) throws Exception {
        mvc.perform(delete(rota + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(rota + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void fluxoCompletoDasNovasEntidades() throws Exception {
        long empresa = criar("/api/empresas", "{\"nome\":\"Oficina Teste\"}");
        long cliente = criar("/api/usuarios", "{\"nome\":\"Pessoa Teste\",\"tipo\":\"CLIENTE\",\"empresa\":{\"id\":" + empresa + "}}");
        long veiculo = criar("/api/veiculos", "{\"placa\":\"ABC1D23\",\"proprietario\":{\"id\":" + cliente + "}}");
        long mercadoria = criar("/api/mercadorias", "{\"nome\":\"Filtro\",\"preco\":39.90,\"empresa\":{\"id\":" + empresa + "}}");
        long servico = criar("/api/servicos", "{\"nome\":\"Troca de oleo\",\"preco\":79.90,\"empresa\":{\"id\":" + empresa + "}}");
        long venda = criar("/api/vendas", "{\"valorTotal\":119.80,\"cliente\":{\"id\":" + cliente + "},\"empresa\":{\"id\":" + empresa + "},\"veiculo\":{\"id\":" + veiculo + "}}");
        long item = criar("/api/itemvendas", "{\"venda\":{\"id\":" + venda + "},\"mercadoria\":{\"id\":" + mercadoria + "},\"quantidade\":1,\"precoUnitario\":39.90}");
        long prestado = criar("/api/servicoprestados", "{\"venda\":{\"id\":" + venda + "},\"servico\":{\"id\":" + servico + "},\"veiculo\":{\"id\":" + veiculo + "},\"valor\":79.90}");
        long credencial = criar("/api/credencials", "{\"login\":\"teste\",\"senhaHash\":\"segredo-de-teste\",\"usuario\":{\"id\":" + cliente + "}}");

        String[] rotas = {"/api/empresas", "/api/usuarios", "/api/veiculos", "/api/mercadorias", "/api/servicos", "/api/vendas", "/api/itemvendas", "/api/servicoprestados", "/api/credencials"};
        long[] ids = {empresa, cliente, veiculo, mercadoria, servico, venda, item, prestado, credencial};
        for (int i = 0; i < rotas.length; i++) conferir(rotas[i], ids[i]);

        atualizar("/api/empresas", empresa, "{\"nome\":\"Nova Oficina\"}");
        atualizar("/api/usuarios", cliente, "{\"nome\":\"Outro nome\",\"tipo\":\"FUNCIONARIO\",\"empresa\":{\"id\":" + empresa + "}}");
        atualizar("/api/veiculos", veiculo, "{\"placa\":\"XYZ1A23\",\"proprietario\":{\"id\":" + cliente + "}}");
        atualizar("/api/mercadorias", mercadoria, "{\"nome\":\"Filtro novo\",\"empresa\":{\"id\":" + empresa + "}}");
        atualizar("/api/servicos", servico, "{\"nome\":\"Revisao\",\"empresa\":{\"id\":" + empresa + "}}");
        atualizar("/api/vendas", venda, "{\"valorTotal\":120.00,\"cliente\":{\"id\":" + cliente + "},\"empresa\":{\"id\":" + empresa + "},\"veiculo\":{\"id\":" + veiculo + "}}");
        atualizar("/api/itemvendas", item, "{\"venda\":{\"id\":" + venda + "},\"mercadoria\":{\"id\":" + mercadoria + "},\"quantidade\":2}");
        atualizar("/api/servicoprestados", prestado, "{\"venda\":{\"id\":" + venda + "},\"servico\":{\"id\":" + servico + "},\"veiculo\":{\"id\":" + veiculo + "}}");
        atualizar("/api/credencials", credencial, "{\"login\":\"outro-login\",\"senhaHash\":\"outra-senha\",\"usuario\":{\"id\":" + cliente + "}}");
        mvc.perform(get("/api/credencials/" + credencial)).andExpect(status().isOk())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // Excluir os dependentes antes das entidades referenciadas.
        excluir("/api/credencials", credencial);
        excluir("/api/servicoprestados", prestado);
        excluir("/api/itemvendas", item);
        excluir("/api/vendas", venda);
        excluir("/api/servicos", servico);
        excluir("/api/mercadorias", mercadoria);
        excluir("/api/veiculos", veiculo);
        excluir("/api/usuarios", cliente);
        excluir("/api/empresas", empresa);
    }

    @Test
    void recusaReferenciaInexistenteETipoInvalido() throws Exception {
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"cliente\":{\"id\":999999}}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Teste\",\"tipo\":\"INDEFINIDO\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/vendas/999999").contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void credencialNaoExibeSenhaNasRespostas() throws Exception {
        long usuario = criar("/api/usuarios", "{\"nome\":\"Seguranca\",\"tipo\":\"CLIENTE\"}");
        long credencial = criar("/api/credencials", "{\"login\":\"teste-senha\",\"senhaHash\":\"segredo\",\"usuario\":{\"id\":" + usuario + "}}");
        mvc.perform(get("/api/credencials/" + credencial))
            .andExpect(status().isOk()).andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get("/api/credencials"))
            .andExpect(status().isOk()).andExpect(content().string(not(containsString("segredo"))));
    }
}
