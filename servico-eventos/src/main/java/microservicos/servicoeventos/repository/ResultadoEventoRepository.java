// repository/ResultadoEventoRepository.java
package microservicos.servicoeventos.repository;

import microservicos.servicoeventos.domain.model.ResultadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResultadoEventoRepository extends JpaRepository<ResultadoEvento, Long> {
    List<ResultadoEvento> findByEventoIdOrderByColocacaoAsc(Long eventoId);
}