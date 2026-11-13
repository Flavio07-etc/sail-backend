package com.faculdade.sail.dtos;

import java.math.BigDecimal;
import java.util.List;

public record PaginaHistoricoResposta(List<RegistroResposta> itens, String proximoCursor, BigDecimal totalDoMes) {}
