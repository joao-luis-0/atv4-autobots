package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.repositorios.EmpresaRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaControle {
 private final EmpresaRepositorio repositorio;
 private final ReferenciaValidador validador;
 public EmpresaControle(EmpresaRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Empresa links(Empresa item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EmpresaControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(EmpresaControle.class).listar()).withRel("lista"));
  return item;
 }
 @GetMapping public List<Empresa> listar() { List<Empresa> itens=repositorio.findAll(); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Empresa> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> ResponseEntity.ok(links(x))).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Empresa> criar(@RequestBody Empresa item) {item.setId(null);  return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Empresa> atualizar(@PathVariable Long id, @RequestBody Empresa item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); item.setId(id);return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
