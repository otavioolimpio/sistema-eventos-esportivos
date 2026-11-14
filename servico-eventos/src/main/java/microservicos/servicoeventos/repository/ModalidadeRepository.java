// repository/ModalidadeRepository.java
package microservicos.servicoeventos.repository;

import microservicos.servicoeventos.domain.model.Modalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {
    Optional<Modalidade> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
}