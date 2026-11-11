package microservicos.servicousuariosequipes.service;

import microservicos.servicousuariosequipes.client.NotificacaoClient;
import microservicos.servicousuariosequipes.client.NotificacaoGateway;
import microservicos.servicousuariosequipes.domain.enums.TipoPerfil;
import microservicos.servicousuariosequipes.domain.model.Usuario;
import microservicos.servicousuariosequipes.dto.NotificacaoRequestDTO;
import microservicos.servicousuariosequipes.dto.NotificacaoResponseDTO;
import microservicos.servicousuariosequipes.repository.UsuarioRepository;
import microservicos.servicousuariosequipes.service.excecoes.ConflitoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** UC02 + «include» UC07: criar usuario dispara o e-mail com as credenciais. */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Pattern SENHA_NO_EMAIL = Pattern.compile("Senha provisoria: (\\S+)");

    @Mock
    private UsuarioRepository repositorio;

    @Mock
    private NotificacaoClient notificacaoClient;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // custo baixo para o teste ser rapido
    private UsuarioService servico;

    @BeforeEach
    void setUp() {
        servico = new UsuarioService(repositorio, new NotificacaoGateway(notificacaoClient), encoder);
        lenient().when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
    }

    @Test
    void criarUsuario_enviaCredenciaisPorEmailParaOProprioUsuario() {
        when(notificacaoClient.enviarNotificacao(any()))
                .thenReturn(new NotificacaoResponseDTO("n-1", "ENVIADA", null));

        UsuarioService.UsuarioCriado resultado = servico.criarUsuario(novoUsuario("ana@exemplo.com"));

        ArgumentCaptor<NotificacaoRequestDTO> captor = ArgumentCaptor.forClass(NotificacaoRequestDTO.class);
        verify(notificacaoClient).enviarNotificacao(captor.capture());
        NotificacaoRequestDTO enviada = captor.getValue();

        assertThat(enviada.canal()).isEqualTo("EMAIL");
        assertThat(enviada.destinatario()).isEqualTo("ana@exemplo.com");
        assertThat(enviada.assunto()).isEqualTo(UsuarioService.ASSUNTO_CREDENCIAIS);
        assertThat(enviada.mensagem()).contains("ana@exemplo.com");

        // A senha que vai no e-mail e a mesma que ficou gravada (como hash, nunca em texto puro)
        String senha = extrairSenha(enviada.mensagem());
        assertThat(resultado.usuario().getSenhaHash()).isNotEqualTo(senha);
        assertThat(encoder.matches(senha, resultado.usuario().getSenhaHash())).isTrue();

        assertThat(resultado.credenciaisEnviadas()).isTrue();
    }

    @Test
    void criarUsuario_quandoServicoDeNotificacoesRegistraFalha_mantemCadastro() {
        when(notificacaoClient.enviarNotificacao(any()))
                .thenReturn(new NotificacaoResponseDTO("n-2", "FALHA", "SMTP fora do ar"));

        UsuarioService.UsuarioCriado resultado = servico.criarUsuario(novoUsuario("bia@exemplo.com"));

        verify(repositorio).save(any(Usuario.class));
        assertThat(resultado.usuario().getId()).isEqualTo(1L);
        assertThat(resultado.credenciaisEnviadas()).isFalse();
    }

    @Test
    void criarUsuario_quandoServicoDeNotificacoesEstaForaDoAr_mantemCadastro() {
        when(notificacaoClient.enviarNotificacao(any()))
                .thenThrow(new RuntimeException("Connection refused"));

        UsuarioService.UsuarioCriado resultado = servico.criarUsuario(novoUsuario("caio@exemplo.com"));

        verify(repositorio).save(any(Usuario.class));
        assertThat(resultado.credenciaisEnviadas()).isFalse();
    }

    @Test
    void criarUsuario_comEmailRepetido_naoSalvaNemNotifica() {
        when(repositorio.findByEmail("dani@exemplo.com")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> servico.criarUsuario(novoUsuario("dani@exemplo.com")))
                .isInstanceOf(ConflitoException.class);

        verify(repositorio, never()).save(any());
        verifyNoInteractions(notificacaoClient);
    }

    private static Usuario novoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Fulano");
        usuario.setEmail(email);
        usuario.setCpf("123.456.789-00");
        usuario.setTipoPerfil(TipoPerfil.JOGADOR);
        return usuario;
    }

    static String extrairSenha(String mensagem) {
        Matcher m = SENHA_NO_EMAIL.matcher(mensagem);
        assertThat(m.find()).as("a mensagem deve conter a senha provisoria").isTrue();
        return m.group(1);
    }
}
