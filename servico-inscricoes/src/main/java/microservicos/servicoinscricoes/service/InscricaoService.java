package microservicos.servicoinscricoes.service;

import microservicos.servicoinscricoes.domain.enums.StatusInscricao;
import microservicos.servicoinscricoes.domain.model.Inscricao;
import microservicos.servicoinscricoes.dto.EquipeDTO;
import microservicos.servicoinscricoes.dto.EventoDTO;
import microservicos.servicoinscricoes.dto.NotificacaoRequestDTO;
import microservicos.servicoinscricoes.dto.SolicitarInscricaoRequestDTO;
import microservicos.servicoinscricoes.dto.UsuarioDTO;
import microservicos.servicoinscricoes.repository.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negocio e persistencia da Inscricao (UC05 e UC06).
 *
 * Este service NAO chama outros microsservicos: quem busca equipe, evento e
 * usuarios e quem dispara a notificacao e a {@link microservicos.servicoinscricoes.facade.InscricaoFacade}.
 * Aqui so entram os dados ja obtidos, as validacoes e o acesso ao banco.
 */
@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;

    public InscricaoService(InscricaoRepository inscricaoRepository) {
        this.inscricaoRepository = inscricaoRepository;
    }

    // ---------------------------------------------------------------
    // UC05 - Solicitar Inscricao em Evento
    // ---------------------------------------------------------------

    /** Somente o chefe da equipe pode solicitar a inscricao. */
    public void validarChefe(EquipeDTO equipe, Long chefeIdInformado) {
        Long chefeRealId = equipe.resolverChefeId();
        if (chefeRealId != null && !chefeRealId.equals(chefeIdInformado)) {
            throw new IllegalArgumentException("Apenas o chefe da equipe pode solicitar inscricao.");
        }
    }

    /** Evento ABERTO, com vaga e da mesma modalidade da equipe; equipe ainda nao inscrita. */
    public void validarEventoParaSolicitacao(EquipeDTO equipe, EventoDTO evento) {
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
                equipe.getId(),
                evento.getId(),
                List.of(StatusInscricao.PENDENTE, StatusInscricao.APROVADA)
        );
        if (jaInscrita) {
            throw new IllegalArgumentException("Esta equipe ja possui uma inscricao pendente ou aprovada neste evento.");
        }
    }

    /** Grava a inscricao com status PENDENTE. */
    public Inscricao registrarPendente(SolicitarInscricaoRequestDTO request) {
        Inscricao inscricao = new Inscricao();
        inscricao.setEquipeId(request.getEquipeId());
        inscricao.setEventoId(request.getEventoId());
        inscricao.setChefeEquipeId(request.getChefeId());
        inscricao.setStatus(StatusInscricao.PENDENTE);
        return inscricaoRepository.save(inscricao);
    }

    // ---------------------------------------------------------------
    // UC06 - Aprovar/Rejeitar Inscricao
    // ---------------------------------------------------------------

    /** Le a inscricao; ela precisa existir e ainda estar PENDENTE. */
    public Inscricao buscarPendente(Long inscricaoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Inscricao nao encontrada: " + inscricaoId));

        if (inscricao.getStatus() != StatusInscricao.PENDENTE) {
            throw new IllegalArgumentException("Inscricao ja foi avaliada anteriormente (" + inscricao.getStatus() + ").");
        }
        return inscricao;
    }

    /** Somente usuarios com perfil de Organizador avaliam inscricoes. */
    public void validarAvaliador(UsuarioDTO avaliador) {
        if (avaliador.getTipoPerfil() != null && !avaliador.getTipoPerfil().toUpperCase().contains("ORGANIZADOR")) {
            throw new IllegalArgumentException("Apenas usuarios com perfil de Organizador podem avaliar inscricoes.");
        }
    }

    /** Para aprovar, o evento ainda precisa ter vaga. */
    public void validarVagaParaAprovacao(EventoDTO evento) {
        if (evento.getVagasDisponiveis() != null && evento.getVagasDisponiveis() <= 0) {
            throw new IllegalArgumentException("Nao ha mais vagas disponiveis no evento para aprovar esta inscricao.");
        }
    }

    /** Grava o resultado da avaliacao (APROVADA ou REJEITADA). */
    public Inscricao registrarAvaliacao(Inscricao inscricao, boolean aprovada, Long organizadorId, String justificativa) {
        inscricao.setStatus(aprovada ? StatusInscricao.APROVADA : StatusInscricao.REJEITADA);
        inscricao.setOrganizadorAvaliadorId(organizadorId);
        inscricao.setJustificativa(justificativa);
        inscricao.setDataAvaliacao(LocalDateTime.now());
        return inscricaoRepository.save(inscricao);
    }

    /** Monta o e-mail (UC07) que avisa o chefe da equipe sobre o resultado. */
    public NotificacaoRequestDTO montarNotificacaoResultado(Inscricao inscricao, UsuarioDTO chefe,
                                                            EquipeDTO equipe, EventoDTO evento) {
        String statusTexto = inscricao.getStatus() == StatusInscricao.APROVADA ? "APROVADA" : "REJEITADA";
        String assunto = "Inscricao " + statusTexto + " - " + evento.getNome();
        String corpo = "Ola " + chefe.getNome() + ", a inscricao da equipe '" + equipe.getNome()
                + "' no evento '" + evento.getNome() + "' foi " + statusTexto + "."
                + (inscricao.getJustificativa() != null ? " Observacao: " + inscricao.getJustificativa() : "");
        return new NotificacaoRequestDTO("EMAIL", chefe.getEmail(), assunto, corpo);
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    public List<Inscricao> listarTodas() {
        return inscricaoRepository.findAll();
    }

    public List<Inscricao> listarPorEvento(Long eventoId) {
        return inscricaoRepository.findByEventoId(eventoId);
    }
}
