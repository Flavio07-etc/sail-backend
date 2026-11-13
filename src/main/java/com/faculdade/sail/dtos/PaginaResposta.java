package com.faculdade.sail.dtos;

import java.util.List;

public record PaginaResposta<T>(List<T> itens, String proximoCursor) {}
