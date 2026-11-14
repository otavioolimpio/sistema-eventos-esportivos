package microservicos.servicoeventos.service;

import feign.FeignException;
import microservicos.servicoeventos.client.UsuarioEquipeClient;
import microservicos.servicoeventos.domain.enums.StatusEvento;
import microservicos.servicoeventos.domain.model.Evento;
import microservicos.servicoeventos.domain.model.Modalidade;
import microservicos.servicoeventos.domain.model.ResultadoEvento;
import microservicos.servicoeventos.dto.CriarEventoRequestDTO;
import microservicos.servicoeventos.dto.CriarModalidadeRequestDTO;
import microservicos.servicoeventos.dto.RegistrarResultadoRequestDTO;
import microservicos.servicoeventos.dto.UsuarioDTO;
import microservicos.servicoeventos.repository.EventoRepository;
import microservicos.servicoeventos.repository.ModalidadeRepository;
import microservicos.servicoeventos.repository.ResultadoEventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoService {

    private final ModalidadeRepository modalidadeRepository;
    private final EventoRepository eventoRepository;
    private final ResultadoEventoRepository resultadoRepository;
    private final UsuarioEquipeClient usuarioEquipeClient;

    public EventoService(
            ModalidadeRepository modalidadeRepository,
            EventoRepository eventoRepository,
            ResultadoEventoRepository resultadoRepository,
            UsuarioEquipeClient usuarioEquipeClient) {
        this.modalidadeRepository = modalidadeRepository;
        this.eventoRepository = eventoRepository;
        this.resultadoRepository = resultadoRepository;
        this.usuarioEquipeClient = usuarioEquipeClient;
    }

    // UC01 - Cadastrar Modalidade
    public Modalidade cadastrarModalidade(CriarModalidadeRequestDTO request) {
        validarOrganizador(request.getOrganizadorId());

        String nomeNormalizado = request.getNome().trim().toUpperCase();
        if (modalidadeRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new IllegalArgumentException("Modalidade ja cadastrada: " + nomeNormalizado);
        }

        Modalidade modalidade = new Modalidade();
        modalidade.setNome(nomeNormalizado);
        modalidade.setDescricao(request.getDescricao());
        modalidade.setMinJogadores(request.getMinJogadores());
        modalidade.setMaxJogadores(request.getMaxJogadores());

        return modalidadeRepository.save(modalidade);
    }

    public List<Modalidade> listarModalidades() {
        return modalidadeRepository.findAll();
    }

    // UC04 - Gerenciar Evento (Criar)
    public Evento criarEvento(CriarEventoRequestDTO request) {
        validarOrganizador(request.getOrganizadorId());

        Modalidade modalidade = modalidadeRepository.findByNomeIgnoreCase(request.getModalidade().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modalidade '" + request.getModalidade() + "' nao encontrada. Cadastre a modalidade primeiro (UC01)."));

        Evento evento = new Evento();
        evento.setNome(request.getNome());
        evento.setDescricao(request.getDescricao());
        evento.setModalidade(modalidade.getNome());
        evento.setOrganizadorId(request.getOrganizadorId());
        evento.setVagasTotais(request.getVagasTotais());
        evento.setVagasDisponiveis(request.getVagasTotais());
        evento.setStatus(StatusEvento.ABERTO);

        return eventoRepository.save(evento);
    }

    public Evento buscarEventoPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado: " + id));
    }

    public List<Evento> listarEventos(String modalidade) {
        if (modalidade != null && !modalidade.isBlank()) {
            return eventoRepository.findByModalidadeIgnoreCase(modalidade);
        }
        return eventoRepository.findAll();
    }

    public Evento atualizarStatusEvento(Long eventoId, Long organizadorId, StatusEvento novoStatus) {
        validarOrganizador(organizadorId);
        Evento evento = buscarEventoPorId(eventoId);
        evento.setStatus(novoStatus);
        return eventoRepository.save(evento);
    }

    // Contrato consumido pelo servico-inscricoes (UC06)
    public Evento ocuparVaga(Long eventoId) {
        Evento evento = buscarEventoPorId(eventoId);
        if (evento.getVagasDisponiveis() <= 0) {
            throw new IllegalArgumentException("Nao ha vagas disponiveis neste evento.");
        }
        evento.setVagasDisponiveis(evento.getVagasDisponiveis() - 1);
        return eventoRepository.save(evento);
    }

    // UC04 - Registrar Resultado
    public ResultadoEvento registrarResultado(Long eventoId, RegistrarResultadoRequestDTO request) {
        validarOrganizador(request.getOrganizadorId());
        Evento evento = buscarEventoPorId(eventoId);

        ResultadoEvento resultado = new ResultadoEvento();
        resultado.setEventoId(evento.getId());
        resultado.setEquipeId(request.getEquipeId());
        resultado.setColocacao(request.getColocacao());
        resultado.setPontuacao(request.getPontuacao());
        resultado.setObservacao(request.getObservacao());

        if (Boolean.TRUE.equals(request.getEncerrarEvento())) {
            evento.setStatus(StatusEvento.FINALIZADO);
            eventoRepository.save(evento);
        }

        return resultadoRepository.save(resultado);
    }

    // UC04-SF03 - Consultar Resultado
    public List<ResultadoEvento> consultarResultados(Long eventoId) {
        buscarEventoPorId(eventoId);
        return resultadoRepository.findByEventoIdOrderByColocacaoAsc(eventoId);
    }

    private void validarOrganizador(Long organizadorId) {
        try {
            UsuarioDTO usuario = usuarioEquipeClient.buscarUsuarioPorId(organizadorId);
            if (usuario.getTipoPerfil() != null && !usuario.getTipoPerfil().toUpperCase().contains("ORGANIZADOR")) {
                throw new IllegalArgumentException("Apenas usuarios com perfil de Organizador podem executar esta operacao.");
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Organizador nao encontrado: " + organizadorId);
        }
    }
}