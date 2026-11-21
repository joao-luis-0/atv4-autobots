package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class ServicoPrestado extends RepresentationModel<ServicoPrestado> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Venda venda;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Servico servico;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Veiculo veiculo;
 @Column private java.math.BigDecimal valor;
}
