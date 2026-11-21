package com.autobots.automanager.seguranca;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.repositorios.*;

@Component
public class Permissoes {
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CredencialRepositorio credenciais;
    @Autowired private VendaRepositorio vendas;

    public boolean perfil(String nome) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + nome));
    }
    public boolean gestao() { return perfil("ADMINISTRADOR") || perfil("GERENTE"); }
    public Usuario atual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario)) throw new AccessDeniedException("Usuario nao identificado");
        return (Usuario) principal;
    }
    public void exigir(boolean permitido) {
        if (!permitido) throw new AccessDeniedException("Operacao nao autorizada para este usuario");
    }
    public boolean gerenciar(Usuario alvo) {
        if (alvo == null || alvo.getPerfil() == null) return false;
        return perfil("ADMINISTRADOR") || (perfil("GERENTE") && alvo.getPerfil() != PerfilUsuario.ADMINISTRADOR)
            || (perfil("VENDEDOR") && alvo.getPerfil() == PerfilUsuario.CLIENTE);
    }
    public boolean lerUsuario(Usuario alvo) {
        return gerenciar(alvo) || (perfil("CLIENTE") && alvo.getId().equals(atual().getId()));
    }
    public void alterarUsuario(Long id, Usuario novo) {
        exigir(gerenciar(novo));
        if (id != null) usuarios.findById(id).ifPresent(u -> exigir(gerenciar(u)));
    }
    public void credencial(Long id, Credencial nova) {
        if (id != null) credenciais.findById(id).ifPresent(c -> exigir(gerenciar(c.getUsuario())));
        if (nova != null) {
            if (nova.getLogin() == null || nova.getLogin().isBlank() || nova.getUsuario() == null || nova.getUsuario().getId() == null)
                throw new IllegalArgumentException("Informe login e usuario da credencial");
            Usuario alvo = usuarios.findById(nova.getUsuario().getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario inexistente"));
            exigir(gerenciar(alvo));
            nova.setUsuario(alvo);
        }
    }
    public boolean lerVenda(Venda venda) {
        return gestao() || (perfil("VENDEDOR") && venda.getVendedor() != null && venda.getVendedor().getId().equals(atual().getId()))
            || (perfil("CLIENTE") && venda.getCliente() != null && venda.getCliente().getId().equals(atual().getId()));
    }
    public Venda venda(Venda referencia) {
        if (referencia == null || referencia.getId() == null) throw new IllegalArgumentException("Informe a venda");
        return vendas.findById(referencia.getId()).orElseThrow(() -> new IllegalArgumentException("Venda inexistente"));
    }
    public void criarVenda(Venda venda) {
        exigir(gestao() || perfil("VENDEDOR"));
        if (perfil("VENDEDOR")) {
            if (venda.getVendedor() != null) exigir(atual().getId().equals(venda.getVendedor().getId()));
            venda.setVendedor(atual());
        }
    }
}
