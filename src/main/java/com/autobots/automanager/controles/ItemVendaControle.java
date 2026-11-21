package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.ItemVenda;
import com.autobots.automanager.repositorios.ItemVendaRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.entidades.Mercadoria;

@RestController
@RequestMapping("/api/itemvendas")
public class ItemVendaControle {
 private final ItemVendaRepositorio repositorio;
 private final ReferenciaValidador validador;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.seguranca.Permissoes permissoes;
 public ItemVendaControle(ItemVendaRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private ItemVenda links(ItemVenda item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ItemVendaControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ItemVendaControle.class).listar()).withRel("lista"));
  return item;
 }
 private ResponseEntity<ItemVenda> respostaAutorizada(ItemVenda item) {permissoes.exigir(item.getVenda() != null && permissoes.lerVenda(item.getVenda())); return ResponseEntity.ok(links(item));}
 @GetMapping public List<ItemVenda> listar() { List<ItemVenda> itens=repositorio.findAll(); itens.removeIf(i -> i.getVenda() == null || !permissoes.lerVenda(i.getVenda())); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<ItemVenda> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> respostaAutorizada(x)).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<ItemVenda> criar(@RequestBody ItemVenda item) {permissoes.exigir(permissoes.gestao() || (permissoes.perfil("VENDEDOR") && permissoes.lerVenda(permissoes.venda(item.getVenda())))); item.setId(null); validador.exigir(item.getVenda(), Venda.class, "venda"); validador.exigir(item.getMercadoria(), Mercadoria.class, "mercadoria"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<ItemVenda> atualizar(@PathVariable Long id, @RequestBody ItemVenda item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); item.setId(id);validador.exigir(item.getVenda(), Venda.class, "venda"); validador.exigir(item.getMercadoria(), Mercadoria.class, "mercadoria");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
