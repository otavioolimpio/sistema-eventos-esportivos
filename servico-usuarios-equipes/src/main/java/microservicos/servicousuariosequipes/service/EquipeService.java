package microservicos.servicousuariosequipes.service;

import microservicos.servicousuariosequipes.domain.model.Equipe;
import microservicos.servicousuariosequipes.domain.strategy.ContextoValidacaoElenco;
import microservicos.servicousuariosequipes.repository.EquipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final ContextoValidacaoElenco contextoValidacaoElenco;

    public EquipeService(EquipeRepository equipeRepository, ContextoValidacaoElenco contextoValidacaoElenco) {
        this.equipeRepository = equipeRepository;
        this.contextoValidacaoElenco = contextoValidacaoElenco;
    }

    @Transactional
    public Equipe criarEquipe(Equipe equipe) {
        contextoValidacaoElenco.executarValidacao(equipe);
        return equipeRepository.save(equipe);
    }

    public List<Equipe> listarTodas() {
        return equipeRepository.findAll();
    }
}