package com.faculdade.sail.dtos;

import com.faculdade.sail.model.Registro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroResposta(Long id, ProdutoResposta produto, Integer quantidade, BigDecimal valorTotal,
        LocalDateTime dataHora, UsuarioResposta usuario, String acao) {

    public static RegistroResposta de(Registro registro) {
        return new RegistroResposta(registro.getId(), ProdutoResposta.de(registro.getProduto()), registro.getQuantidade(),
                registro.getValorTotal(), registro.getDataVenda(), UsuarioResposta.de(registro.getUsuario()), registro.getAcao());
    }
}
