package com.faculdade.sail.dtos;

import java.math.BigDecimal;
import java.util.List;

public class HistoricoDTO {

    private BigDecimal totalDoMes;
    private List<RegistroResposta> lancamentos;

    public HistoricoDTO(BigDecimal totalDoMes, List<RegistroResposta> lancamentos) {
        this.totalDoMes = totalDoMes;
        this.lancamentos = lancamentos;
    }

    // Getters
    public BigDecimal getTotalDoMes() { return totalDoMes; }
    public void setTotalDoMes(BigDecimal totalDoMes) { this.totalDoMes = totalDoMes; }

    public List<RegistroResposta> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<RegistroResposta> lancamentos) { this.lancamentos = lancamentos; }
}
