package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class Veiculo extends RepresentationModel<Veiculo> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column private String placa;
 @Column private String modelo;
 @Column private String marca;
 @Column private Integer ano;
 @ManyToOne @JsonIgnoreProperties({"links", "email", "telefone", "empresa", "tipo", "perfil"}) private Usuario proprietario;
}
