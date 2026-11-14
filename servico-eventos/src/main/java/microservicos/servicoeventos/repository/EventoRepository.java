// repository/EventoRepository.java
package microservicos.servicoeventos.repository;

import microservicos.servicoeventos.domain.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByModalidadeIgnoreCase(String modalidade);
}