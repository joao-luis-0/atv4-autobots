package com.autobots.automanager.repositorios;
import com.autobots.automanager.entidades.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CredencialRepositorio extends JpaRepository<Credencial, Long> {
 java.util.Optional<Credencial> findByLogin(String login);
 void deleteByUsuarioId(Long usuarioId);
}
