package microservicos.servicousuariosequipes.service;

import microservicos.servicousuariosequipes.client.NotificacaoGateway;
import microservicos.servicousuariosequipes.domain.enums.TipoPerfil;
import microservicos.servicousuariosequipes.domain.model.Equipe;
import microservicos.servicousuariosequipes.domain.model.Usuario;
import microservicos.servicousuariosequipes.domain.strategy.ContextoValidacaoElenco;
import microservicos.servicousuariosequipes.dto.EquipeRequest;
import microservicos.servicousuariosequipes.repository.EquipeRepository;
import microservicos.servicousuariosequipes.repository.UsuarioRepository;
import microservicos.servicousuariosequipes.service.excecoes.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContextoValidacaoElenco contextoValidacaoElenco;
    private final NotificacaoGateway notificacoes;

    public EquipeService(EquipeRepository equipeRepository,
                         UsuarioRepository usuarioRepository,
                         ContextoValidacaoElenco contextoValidacaoElenco,
                         NotificacaoGateway notificacoes) {
        this.equipeRepository = equipeRepository;
        this.usuarioRepository = usuarioRepository;
        this.contextoValidacaoElenco = contextoValidacaoElenco;
        this.notificacoes = notificacoes;
    }

    /** UC03 - Cadastrar Equipe: valida o elenco (Strategy), salva e avisa o chefe. */
    public Equipe criarEquipe(EquipeRequest requisicao) {
        Usuario chefe = usuarioRepository.findById(requisicao.chefeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chefe nao encontrado: " + requisicao.chefeId()));
        if (chefe.getTipoPerfil() != TipoPerfil.CHEFE_EQUIPE) {
            throw new IllegalArgumentException("O usuario " + chefe.getId() + " nao tem perfil CHEFE_EQUIPE.");
        }

        List<Long> ids = requisicao.jogadoresIds().stream().distinct().toList();
        List<Usuario> jogadores = usuarioRepository.findAllById(ids);
        if (jogadores.size() != ids.size()) {
            throw new RecursoNaoEncontradoException("Um ou mais jogadores informados nao existem.");
        }

        Equipe equipe = new Equipe();
        equipe.setNome(requisicao.nome());
        equipe.setModalidade(requisicao.modalidade());
        equipe.setChefeEquipe(chefe);
        equipe.setJogadores(new ArrayList<>(jogadores));

        contextoValidacaoElenco.executarValidacao(equipe);
        Equipe salva = equipeRepository.save(equipe);

        notificacoes.enviarEmail(
                chefe.getEmail(),
                "Equipe " + salva.getNome() + " criada",
                "Ola, " + chefe.getNome() + "! A equipe " + salva.getNome() + " (" + salva.getModalidade()
                        + ") foi criada com sucesso, com " + salva.getJogadores().size() + " jogador(es).");

        return salva;
    }

    public List<Equipe> listarTodas() {
        return equipeRepository.findAll();
    }

    public Optional<Equipe> buscarPorId(Long id) {
        return equipeRepository.findById(id);
    }
}
