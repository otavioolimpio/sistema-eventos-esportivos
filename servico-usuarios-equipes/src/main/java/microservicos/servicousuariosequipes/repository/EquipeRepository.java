package microservicos.servicousuariosequipes.repository;

import microservicos.servicousuariosequipes.domain.model.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
}