package com.faculdade.sail.service;

import com.faculdade.sail.dtos.HistoricoDTO;
import com.faculdade.sail.dtos.PaginaHistoricoResposta;
import com.faculdade.sail.dtos.RegistroResposta;
import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.repository.RegistroRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HistoricoService {

    private final RegistroRepository registroRepository;

    public HistoricoService(RegistroRepository registroRepository) {
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public HistoricoDTO obterHistorico(int mes, int ano) {
        validarPeriodo(mes, ano);
        List<Registro> lancamentos = mes == 0 ? registroRepository.findAllByOrderByDataVendaDescIdDesc()
                : registroRepository.buscarVendasPorMesEAno(mes, ano);
        BigDecimal total = lancamentos.stream()
                .filter(registro -> "VENDA".equals(registro.getAcao()) && registro.getValorTotal() != null)
                .map(Registro::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new HistoricoDTO(total, lancamentos.stream().map(RegistroResposta::de).toList());
    }

    @Transactional(readOnly = true)
    public PaginaHistoricoResposta obterPagina(int mes, int ano, int tamanho, String cursor) {
        validarPeriodo(mes, ano);
        Paginacao.validarTamanho(tamanho);
        CursorHistorico anterior = CursorHistorico.decodificar(cursor, mes, ano);
        List<Registro> encontrados = registroRepository.buscarPagina(mes, ano, anterior == null ? null : anterior.data(),
                anterior == null ? null : anterior.id(), PageRequest.of(0, tamanho + 1));
        List<Registro> pagina = encontrados.subList(0, Math.min(tamanho, encontrados.size()));
        String proximo = null;
        if (encontrados.size() > tamanho) {
            Registro ultimo = pagina.getLast();
            proximo = new CursorHistorico(ultimo.getDataVenda(), ultimo.getId()).codificar(mes, ano);
        }
        return new PaginaHistoricoResposta(pagina.stream().map(RegistroResposta::de).toList(), proximo,
                registroRepository.totalizarVendas(mes, ano));
    }

    private void validarPeriodo(int mes, int ano) {
        if ((mes == 0) != (ano == 0) || mes < 0 || mes > 12 || ano < 0 || ano > 9999) {
            throw ApiException.invalida("Informe mês e ano válidos para consultar o histórico.");
        }
    }
}
