package microservicos.servicousuariosequipes.service;

import microservicos.servicousuariosequipes.client.NotificacaoClient;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import microservicos.servicousuariosequipes.domain.strategy.ContextoValidacaoElenco;
import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import microservicos.servicousuariosequipes.repository.EquipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final ContextoValidacaoElenco contextoValidacaoElenco;
    private final NotificacaoClient notificacaoClient;

    public EquipeService(EquipeRepository equipeRepository,
                         ContextoValidacaoElenco contextoValidacaoElenco,
                         NotificacaoClient notificacaoClient) {
        this.equipeRepository = equipeRepository;
        this.contextoValidacaoElenco = contextoValidacaoElenco;
        this.notificacaoClient = notificacaoClient;
    }

    @Transactional
    public Equipe criarEquipe(Equipe equipe) {
        contextoValidacaoElenco.executarValidacao(equipe);
        Equipe equipeSalva = equipeRepository.save(equipe);

        if (equipeSalva.getChefeEquipe() != null && equipeSalva.getChefeEquipe().getEmail() != null) {
            try {
                notificacaoClient.enviarNotificacao(new NotificacaoRequestDTO(
                        equipeSalva.getChefeEquipe().getEmail(),
                        "Equipe " + equipeSalva.getNome() + " criada com sucesso!",
                        "EMAIL"
                ));
            } catch (Exception e) {
                System.err.println("Erro ao enviar notificacao: " + e.getMessage());
            }
        }

        return equipeSalva;
    }

    public List<Equipe> listarTodas() {
        return equipeRepository.findAll();
    }
}