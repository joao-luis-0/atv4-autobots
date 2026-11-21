package com.autobots.automanager.controles;
import com.autobots.automanager.entidades.ServicoPrestado;
import com.autobots.automanager.repositorios.ServicoPrestadoRepositorio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import java.util.*;
import com.autobots.automanager.validacao.ReferenciaValidador;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.entidades.Servico;
import com.autobots.automanager.entidades.Veiculo;

@RestController
@RequestMapping("/api/servicoprestados")
public class ServicoPrestadoControle {
 private final ServicoPrestadoRepositorio repositorio;
 private final ReferenciaValidador validador;
 @org.springframework.beans.factory.annotation.Autowired private com.autobots.automanager.seguranca.Permissoes permissoes;
 public ServicoPrestadoControle(ServicoPrestadoRepositorio repositorio, ReferenciaValidador validador) {this.repositorio=repositorio;this.validador=validador;}
 private ServicoPrestado links(ServicoPrestado item) {
  item.removeLinks();
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ServicoPrestadoControle.class).buscar(item.getId())).withSelfRel());
  item.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ServicoPrestadoControle.class).listar()).withRel("lista"));
  return item;
 }
 private ResponseEntity<ServicoPrestado> respostaAutorizada(ServicoPrestado item) {permissoes.exigir(item.getVenda() != null && permissoes.lerVenda(item.getVenda())); return ResponseEntity.ok(links(item));}
 @GetMapping public List<ServicoPrestado> listar() { List<ServicoPrestado> itens=repositorio.findAll(); itens.removeIf(i -> i.getVenda() == null || !permissoes.lerVenda(i.getVenda())); itens.forEach(this::links); return itens; }
 @GetMapping("/{id}") public ResponseEntity<ServicoPrestado> buscar(@PathVariable Long id) {return repositorio.findById(id).map(x -> respostaAutorizada(x)).orElseGet(() -> ResponseEntity.notFound().build());}
 @PostMapping public ResponseEntity<ServicoPrestado> criar(@RequestBody ServicoPrestado item) {permissoes.exigir(permissoes.gestao() || (permissoes.perfil("VENDEDOR") && permissoes.lerVenda(permissoes.venda(item.getVenda())))); item.setId(null); validador.exigir(item.getVenda(), Venda.class, "venda"); validador.exigir(item.getServico(), Servico.class, "servico"); validador.exigir(item.getVeiculo(), Veiculo.class, "veiculo"); return ResponseEntity.status(HttpStatus.CREATED).body(links(repositorio.save(item)));}
 @PutMapping("/{id}") public ResponseEntity<ServicoPrestado> atualizar(@PathVariable Long id, @RequestBody ServicoPrestado item) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); item.setId(id);validador.exigir(item.getVenda(), Venda.class, "venda"); validador.exigir(item.getServico(), Servico.class, "servico"); validador.exigir(item.getVeiculo(), Veiculo.class, "veiculo");return ResponseEntity.ok(links(repositorio.save(item)));}
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {if(!repositorio.existsById(id))return ResponseEntity.notFound().build(); permissoes.exigir(permissoes.gestao()); repositorio.deleteById(id);return ResponseEntity.noContent().build();}
}
