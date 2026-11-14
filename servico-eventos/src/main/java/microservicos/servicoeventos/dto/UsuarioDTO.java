// dto/UsuarioDTO.java
package microservicos.servicoeventos.dto;

public class UsuarioDTO {
    private Long id;
    private String nome;
    private String tipoPerfil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipoPerfil() { return tipoPerfil; }
    public void setTipoPerfil(String tipoPerfil) { this.tipoPerfil = tipoPerfil; }
}