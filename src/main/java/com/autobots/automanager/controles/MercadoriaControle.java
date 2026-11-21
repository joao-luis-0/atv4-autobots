package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Mercadoria;
import com.autobots.automanager.repositorios.MercadoriaRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Empresa;

@RestController
@RequestMapping("/api/mercadorias")
public class MercadoriaControle {
 private final MercadoriaRepositorio repositorio;
 private final ReferenciaValidador validador;
 public MercadoriaControle(MercadoriaRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Mercadoria links(Mercadoria item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MercadoriaControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MercadoriaControle.class).listar()).withRel("lista"));
  return item;
 }
 @GetMapping public List<Mercadoria> listar() { List<Mercadoria> itens=repositorio.findAll(); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Mercadoria> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> ResponseEntity.ok(links(x))).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Mercadoria> criar(@RequestBody Mercadoria item) {item.setId(null); validador.exigir(item.getEmpresa(), Empresa.class, "empresa"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Mercadoria> atualizar(@PathVariable Long id, @RequestBody Mercadoria item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); item.setId(id);validador.exigir(item.getEmpresa(), Empresa.class, "empresa");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
