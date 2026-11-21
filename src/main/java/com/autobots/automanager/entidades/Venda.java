package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class Venda extends RepresentationModel<Venda> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column private java.time.LocalDateTime data;
 @Column private java.math.BigDecimal valorTotal;
 @ManyToOne @JsonIgnoreProperties({"links", "email", "telefone", "empresa", "tipo", "perfil"}) private Usuario cliente;
 @ManyToOne @JsonIgnoreProperties({"links", "email", "telefone", "empresa", "tipo", "perfil"}) private Usuario vendedor;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Empresa empresa;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Veiculo veiculo;
}
