package microservicos.servicoinscricoes.repository;

import microservicos.servicoinscricoes.domain.enums.StatusInscricao;
import microservicos.servicoinscricoes.domain.model.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    boolean existsByEquipeIdAndEventoIdAndStatusIn(Long equipeId, Long eventoId, List<StatusInscricao> statuses);
    List<Inscricao> findByEventoId(Long eventoId);
    List<Inscricao> findByEquipeId(Long equipeId);
}