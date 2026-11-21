package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.repositorios.UsuarioRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Empresa;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControle {
 private final UsuarioRepositorio repositorio;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.repositorios.CredencialRepositorio credenciais;
 private final ReferenciaValidador validador;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.seguranca.Permissoes permissoes;
 public UsuarioControle(UsuarioRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Usuario links(Usuario item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UsuarioControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UsuarioControle.class).listar()).withRel("lista"));
  return item;
 }
 private ResponseEntity<Usuario> respostaAutorizada(Usuario usuario) {permissoes.exigir(permissoes.lerUsuario(usuario)); return ResponseEntity.ok(links(usuario));}
 @GetMapping public List<Usuario> listar() { List<Usuario> itens=repositorio.findAll(); itens.removeIf(u -> !permissoes.lerUsuario(u)); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Usuario> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> respostaAutorizada(x)).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Usuario> criar(@RequestBody Usuario item) {permissoes.alterarUsuario(null, item); item.setId(null); validador.exigir(item.getEmpresa(), Empresa.class, "empresa"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Usuario> atualizar(@PathVariable Long id, @RequestBody Usuario item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.alterarUsuario(id, item); item.setId(id);validador.exigir(item.getEmpresa(), Empresa.class, "empresa");return ResponseEntity.ok(links(repositorio.save(item)));}
 @org.springframework.transaction.annotation.Transactional
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gerenciar(repositorio.findById(id).orElseThrow())); credenciais.deleteByUsuarioId(id); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
