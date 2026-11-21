package com.autobots.automanager.entidades;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter @Entity
public class Credencial extends RepresentationModel<Credencial> {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, unique = true) private String login;
 @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Column(length = 255) private String senhaHash;
 @ManyToOne(optional = false) @JsonIgnoreProperties({"links"}) private Usuario usuario;
}
