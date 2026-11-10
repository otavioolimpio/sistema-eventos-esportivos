package microservicos.serviconotificacoes.singleton;

import microservicos.serviconotificacoes.adapter.FalhaEnvioException;
import microservicos.serviconotificacoes.adapter.INotificador;
import microservicos.serviconotificacoes.dominio.Canal;
import microservicos.serviconotificacoes.dominio.Mensagem;
import microservicos.serviconotificacoes.dominio.ResultadoEnvio;
import microservicos.serviconotificacoes.dominio.StatusEnvio;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testes de unidade do Singleton e da seleção de adaptadores (sem Spring). */
class NotificationClientManagerTest {

    /** Adaptador falso, usado só nos testes. */
    private static final class AdaptadorFalso implements INotificador {
        private final Canal canal;
        private final boolean falhar;
        private final List<Mensagem> enviadas = new ArrayList<>();

        AdaptadorFalso(Canal canal, boolean falhar) {
            this.canal = canal;
            this.falhar = falhar;
        }

        @Override
        public Canal canal() {
            return canal;
        }

        @Override
        public void enviar(Mensagem mensagem) {
            if (falhar) {
                throw new FalhaEnvioException("provedor fora do ar");
            }
            enviadas.add(mensagem);
        }
    }

    private final Mensagem mensagem = new Mensagem("chefe@exemplo.com", "Inscrição aprovada", "Sua inscrição foi aprovada.");

    // Observação: o Singleton é global. Os adaptadores falsos registrados aqui são
    // substituídos pelos reais no @BeforeEach do NotificacaoControllerTest.

    @Test
    void getInstanceSempreDevolveAMesmaInstancia() {
        assertThat(NotificationClientManager.getInstance())
                .isSameAs(NotificationClientManager.getInstance());
    }

    @Test
    void enviaPeloAdaptadorDoCanal() {
        AdaptadorFalso email = new AdaptadorFalso(Canal.EMAIL, false);
        NotificationClientManager.getInstance().registrar(email);

        ResultadoEnvio resultado = NotificationClientManager.getInstance().enviar(Canal.EMAIL, mensagem);

        assertThat(resultado.status()).isEqualTo(StatusEnvio.ENVIADA);
        assertThat(email.enviadas).containsExactly(mensagem);
    }

    @Test
    void falhaDoProvedorViraResultadoENaoExcecao() {
        NotificationClientManager.getInstance().registrar(new AdaptadorFalso(Canal.DISCORD, true));

        ResultadoEnvio resultado = NotificationClientManager.getInstance().enviar(Canal.DISCORD, mensagem);

        assertThat(resultado.status()).isEqualTo(StatusEnvio.FALHA);
        assertThat(resultado.erro()).contains("provedor fora do ar");
    }
}
