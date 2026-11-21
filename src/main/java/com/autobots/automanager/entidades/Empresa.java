package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class Empresa extends RepresentationModel<Empresa> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column private String nome;
 @Column private String cnpj;
 @Column private String telefone;
 @Column private String email;
 @Column private String endereco;
}
