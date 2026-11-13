package com.faculdade.sail.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "registros", uniqueConstraints = @UniqueConstraint(name = "uk_registro_usuario_chave", columnNames = {"usuario_id", "chave_operacao"}))
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime dataVenda;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String acao;

    @JsonIgnore
    @Column(length = 100)
    private String chaveOperacao;

    @JsonIgnore
    @Column(length = 64)
    private String assinaturaOperacao;

    public Registro() {}

    public Registro(Produto produto, Integer quantidade, BigDecimal valorTotal, Usuario usuario) {
        this(produto, quantidade, valorTotal, usuario, "VENDA");
    }

    public Registro(Produto produto, Integer quantidade, BigDecimal valorTotal, Usuario usuario, String acao) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal;
        this.usuario = usuario;
        this.dataVenda = LocalDateTime.now();
        this.acao = acao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    @JsonProperty("dataHora")
    public LocalDateTime getDataVenda() { return dataVenda; }
    public void setDataVenda(LocalDateTime dataVenda) { this.dataVenda = dataVenda; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getChaveOperacao() { return chaveOperacao; }
    public void setChaveOperacao(String chaveOperacao) { this.chaveOperacao = chaveOperacao; }
    public String getAssinaturaOperacao() { return assinaturaOperacao; }
    public void setAssinaturaOperacao(String assinaturaOperacao) { this.assinaturaOperacao = assinaturaOperacao; }
}
