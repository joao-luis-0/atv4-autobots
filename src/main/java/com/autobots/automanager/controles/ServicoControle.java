package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Servico;
import com.autobots.automanager.repositorios.ServicoRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Empresa;

@RestController
@RequestMapping("/api/servicos")
public class ServicoControle {
 private final ServicoRepositorio repositorio;
 private final ReferenciaValidador validador;
 public ServicoControle(ServicoRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Servico links(Servico item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ServicoControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ServicoControle.class).listar()).withRel("lista"));
  return item;
 }
 @GetMapping public List<Servico> listar() { List<Servico> itens=repositorio.findAll(); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Servico> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> ResponseEntity.ok(links(x))).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Servico> criar(@RequestBody Servico item) {item.setId(null); validador.exigir(item.getEmpresa(), Empresa.class, "empresa"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Servico> atualizar(@PathVariable Long id, @RequestBody Servico item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); item.setId(id);validador.exigir(item.getEmpresa(), Empresa.class, "empresa");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
