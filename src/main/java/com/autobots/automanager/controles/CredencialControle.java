package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.repositorios.CredencialRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Usuario;

@RestController
@RequestMapping("/api/credencials")
public class CredencialControle {
 private final CredencialRepositorio repositorio;
 private final ReferenciaValidador validador;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.seguranca.Permissoes permissoes;
 private final com.autobots.automanager.seguranca.SenhaServico senhaServico;
 public CredencialControle(CredencialRepositorio repositorio, ReferenciaValidador validador, com.autobots.automanager.seguranca.SenhaServico senhaServico) {this.repositorio=repositorio;this.validador=validador;this.senhaServico=senhaServico;}
 private Credencial links(Credencial item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CredencialControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CredencialControle.class).listar()).withRel("lista"));
  return item;
 }
 private ResponseEntity<Credencial> respostaAutorizada(Credencial credencial) {permissoes.exigir(permissoes.gerenciar(credencial.getUsuario())); return ResponseEntity.ok(links(credencial));}
 @GetMapping public List<Credencial> listar() { List<Credencial> itens=repositorio.findAll(); itens.removeIf(c -> !permissoes.gerenciar(c.getUsuario())); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Credencial> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> respostaAutorizada(x)).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Credencial> criar(@RequestBody Credencial item) {permissoes.credencial(null, item); item.setId(null); validador.exigir(item.getUsuario(), Usuario.class, "usuario"); item.setSenhaHash(senhaServico.gerarHash(item.getSenhaHash())); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Credencial> atualizar(@PathVariable Long id, @RequestBody Credencial item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.credencial(id, item); item.setId(id);validador.exigir(item.getUsuario(), Usuario.class, "usuario"); item.setSenhaHash(senhaServico.gerarHash(item.getSenhaHash()));return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.credencial(id, null); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
