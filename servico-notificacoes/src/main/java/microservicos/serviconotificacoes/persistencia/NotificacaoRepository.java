package microservicos.serviconotificacoes.persistencia;

import microservicos.serviconotificacoes.dominio.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Registro das notificações no banco SQLite próprio do serviço (Spring Data JPA). */
public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    /** Notificações mais recentes primeiro; o tamanho da página define o limite. */
    List<Notificacao> findAllByOrderByCriadaEmDesc(Pageable pagina);
}
