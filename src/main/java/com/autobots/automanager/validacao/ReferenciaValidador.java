package com.autobots.automanager.validacao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class ReferenciaValidador {
    @PersistenceContext private EntityManager entityManager;

    public void exigir(Object referencia, Class<?> tipo, String campo) {
        if (referencia == null) return;
        try {
            Object id = tipo.getMethod("getId").invoke(referencia);
            if (id == null || entityManager.find(tipo, id) == null) {
                throw new IllegalArgumentException("Referencia inexistente em " + campo + ": " + id);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Entidade sem identificador", ex);
        }
    }
}
