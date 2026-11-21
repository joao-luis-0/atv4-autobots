package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Veiculo;
import com.autobots.automanager.repositorios.VeiculoRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Usuario;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoControle {
 private final VeiculoRepositorio repositorio;
 private final ReferenciaValidador validador;
 public VeiculoControle(VeiculoRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Veiculo links(Veiculo item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(VeiculoControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(VeiculoControle.class).listar()).withRel("lista"));
  return item;
 }
 @GetMapping public List<Veiculo> listar() { List<Veiculo> itens=repositorio.findAll(); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Veiculo> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> ResponseEntity.ok(links(x))).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Veiculo> criar(@RequestBody Veiculo item) {item.setId(null); validador.exigir(item.getProprietario(), Usuario.class, "proprietario"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Veiculo> atualizar(@PathVariable Long id, @RequestBody Veiculo item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); item.setId(id);validador.exigir(item.getProprietario(), Usuario.class, "proprietario");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
