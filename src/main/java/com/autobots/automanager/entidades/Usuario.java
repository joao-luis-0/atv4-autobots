package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter @Entity
public class Usuario extends RepresentationModel<Usuario> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column private String nome;
 @Column private String email;
 @Enumerated(EnumType.STRING) @Column private TipoUsuario tipo;
 @Column private String telefone;
 @Enumerated(EnumType.STRING) @Column(nullable = false) private PerfilUsuario perfil = PerfilUsuario.CLIENTE;
 @ManyToOne @JsonIgnoreProperties({"links"}) private Empresa empresa;
}
