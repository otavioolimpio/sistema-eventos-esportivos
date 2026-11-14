package microservicos.servicoinscricoes.service;

import feign.FeignException;
import microservicos.servicoinscricoes.client.EventoClient;
import microservicos.servicoinscricoes.client.NotificacaoClient;
import microservicos.servicoinscricoes.client.UsuarioEquipeClient;
import microservicos.servicoinscricoes.domain.enums.StatusInscricao;
import microservicos.servicoinscricoes.domain.model.Inscricao;
import microservicos.servicoinscricoes.dto.*;
import microservicos.servicoinscricoes.repository.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final UsuarioEquipeClient usuarioEquipeClient;
    private final EventoClient eventoClient;
    private final NotificacaoClient notificacaoClient;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            UsuarioEquipeClient usuarioEquipeClient,
            EventoClient eventoClient,
            NotificacaoClient notificacaoClient) {
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioEquipeClient = usuarioEquipeClient;
        this.eventoClient = eventoClient;
        this.notificacaoClient = notificacaoClient;
    }

    // UC05 - Solicitar Inscricao em Evento (Chefe de Equipe)
    public Inscricao solicitarInscricao(SolicitarInscricaoRequestDTO request) {
        EquipeDTO equipe = buscarEquipeValidada(request.getEquipeId());
        Long chefeRealId = equipe.resolverChefeId();

        if (chefeRealId != null && !chefeRealId.equals(request.getChefeId())) {
            throw new IllegalArgumentException("Apenas o chefe da equipe pode solicitar inscricao.");
        }

        EventoDTO evento = buscarEventoValidado(request.getEventoId());

        if (evento.getStatus() != null && !"ABERTO".equalsIgnoreCase(evento.getStatus())) {
            throw new IllegalArgumentException("O evento nao esta aberto para inscricoes.");
        }

        if (evento.getVagasDisponiveis() != null && evento.getVagasDisponiveis() <= 0) {
            throw new IllegalArgumentException("Nao ha vagas disponiveis para este evento.");
        }

        if (evento.getModalidade() != null && equipe.getModalidade() != null
                && !evento.getModalidade().equalsIgnoreCase(equipe.getModalidade())) {
            throw new IllegalArgumentException("Modalidade da equipe (" + equipe.getModalidade()
                    + ") incompativel com o evento (" + evento.getModalidade() + ").");
        }

        boolean jaInscrita = inscricaoRepository.existsByEquipeIdAndEventoIdAndStatusIn(
                request.getEquipeId(),
                request.getEventoId(),
                List.of(StatusInscricao.PENDENTE, StatusInscricao.APROVADA)
        );
        if (jaInscrita) {
            throw new IllegalArgumentException("Esta equipe ja possui uma inscricao pendente ou aprovada neste evento.");
        }

        Inscricao inscricao = new Inscricao();
        inscricao.setEquipeId(request.getEquipeId());
        inscricao.setEventoId(request.getEventoId());
        inscricao.setChefeEquipeId(request.getChefeId());
        inscricao.setStatus(StatusInscricao.PENDENTE);

        return inscricaoRepository.save(inscricao);
    }

    // UC06 - Aprovar/Rejeitar Inscricao (Organizador do Evento) + <<include>> UC07
    public InscricaoResponseDTO avaliarInscricao(Long inscricaoId, AvaliarInscricaoRequestDTO request) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Inscricao nao encontrada: " + inscricaoId));

        if (inscricao.getStatus() != StatusInscricao.PENDENTE) {
            throw new IllegalArgumentException("Inscricao ja foi avaliada anteriormente (" + inscricao.getStatus() + ").");
        }

        UsuarioDTO organizador = buscarUsuarioValidado(request.getOrganizadorId());
        if (organizador.getTipoPerfil() != null && !organizador.getTipoPerfil().toUpperCase().contains("ORGANIZADOR")) {
            throw new IllegalArgumentException("Apenas usuarios com perfil de Organizador podem avaliar inscricoes.");
        }

        EventoDTO evento = buscarEventoValidado(inscricao.getEventoId());
        EquipeDTO equipe = buscarEquipeValidada(inscricao.getEquipeId());

        if (Boolean.TRUE.equals(request.getAprovada())) {
            if (evento.getVagasDisponiveis() != null && evento.getVagasDisponiveis() <= 0) {
                throw new IllegalArgumentException("Nao ha mais vagas disponiveis no evento para aprovar esta inscricao.");
            }
            eventoClient.ocuparVaga(evento.getId());
            inscricao.setStatus(StatusInscricao.APROVADA);
        } else {
            inscricao.setStatus(StatusInscricao.REJEITADA);
        }

        inscricao.setOrganizadorAvaliadorId(request.getOrganizadorId());
        inscricao.setJustificativa(request.getJustificativa());
        inscricao.setDataAvaliacao(LocalDateTime.now());
        inscricao = inscricaoRepository.save(inscricao);

        // <<include>> UC07 - Enviar Notificacao ao Chefe de Equipe (resiliente a falhas de SMTP)
        boolean notificacaoEnviada = false;
        String aviso = null;
        try {
            UsuarioDTO chefe = buscarUsuarioValidado(inscricao.getChefeEquipeId());
            String statusTexto = inscricao.getStatus() == StatusInscricao.APROVADA ? "APROVADA" : "REJEITADA";
            String assunto = "Inscricao " + statusTexto + " - " + evento.getNome();
            String corpo = "Ola " + chefe.getNome() + ", a inscricao da equipe '" + equipe.getNome()
                    + "' no evento '" + evento.getNome() + "' foi " + statusTexto + "."
                    + (inscricao.getJustificativa() != null ? " Observacao: " + inscricao.getJustificativa() : "");

            notificacaoClient.enviarNotificacao(new NotificacaoRequestDTO(
                    "EMAIL",
                    chefe.getEmail(),
                    assunto,
                    corpo
            ));
            notificacaoEnviada = true;
        } catch (Exception e) {
            aviso = "Inscricao atualizada com sucesso, mas houve falha ao enviar notificacao por e-mail.";
        }

        return new InscricaoResponseDTO(inscricao, notificacaoEnviada, aviso);
    }

    public List<Inscricao> listarTodas() {
        return inscricaoRepository.findAll();
    }

    public List<Inscricao> listarPorEvento(Long eventoId) {
        return inscricaoRepository.findByEventoId(eventoId);
    }

    private EquipeDTO buscarEquipeValidada(Long equipeId) {
        try {
            return usuarioEquipeClient.buscarEquipePorId(equipeId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Equipe nao encontrada: " + equipeId);
        }
    }

    private UsuarioDTO buscarUsuarioValidado(Long usuarioId) {
        try {
            return usuarioEquipeClient.buscarUsuarioPorId(usuarioId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Usuario nao encontrado: " + usuarioId);
        }
    }

    private EventoDTO buscarEventoValidado(Long eventoId) {
        try {
            return eventoClient.buscarEventoPorId(eventoId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Evento nao encontrado: " + eventoId);
        }
    }
}