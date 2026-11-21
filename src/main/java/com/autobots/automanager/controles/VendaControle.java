package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.repositorios.VendaRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.entidades.Empresa;
import com.autobots.automanager.entidades.Veiculo;

@RestController
@RequestMapping("/api/vendas")
public class VendaControle {
 private final VendaRepositorio repositorio;
 private final ReferenciaValidador validador;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.seguranca.Permissoes permissoes;
 public VendaControle(VendaRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private Venda links(Venda item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(VendaControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(VendaControle.class).listar()).withRel("lista"));
  return item;
 }
 private ResponseEntity<Venda> respostaAutorizada(Venda venda) {permissoes.exigir(permissoes.lerVenda(venda)); return ResponseEntity.ok(links(venda));}
 @GetMapping public List<Venda> listar() { List<Venda> itens=repositorio.findAll(); itens.removeIf(v -> !permissoes.lerVenda(v)); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<Venda> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> respostaAutorizada(x)).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<Venda> criar(@RequestBody Venda item) {permissoes.criarVenda(item); item.setId(null); validador.exigir(item.getVendedor(), Usuario.class, "vendedor"); validador.exigir(item.getCliente(), Usuario.class, "cliente"); validador.exigir(item.getEmpresa(), Empresa.class, "empresa"); validador.exigir(item.getVeiculo(), Veiculo.class, "veiculo"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<Venda> atualizar(@PathVariable Long id, @RequestBody Venda item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); item.setId(id);validador.exigir(item.getVendedor(), Usuario.class, "vendedor"); validador.exigir(item.getCliente(), Usuario.class, "cliente"); validador.exigir(item.getEmpresa(), Empresa.class, "empresa"); validador.exigir(item.getVeiculo(), Veiculo.class, "veiculo");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
