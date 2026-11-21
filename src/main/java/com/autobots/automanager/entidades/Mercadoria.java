package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class Mercadoria extends RepresentationModel<Mercadoria> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column private String nome;
 @Column private String descricao;
 @Column private java.math.BigDecimal preco;
 @Column private Integer estoque;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Empresa empresa;
}
