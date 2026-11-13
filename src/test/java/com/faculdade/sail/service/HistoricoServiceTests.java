package com.faculdade.sail.service;

import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.model.Produto;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.repository.RegistroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTests {

    @Mock
    private RegistroRepository repository;
    @InjectMocks
    private HistoricoService service;

    @Test
    void consultaPeriodoETotalizaSomenteVendasPreservandoOrdem() {
        Registro venda = registro(3L, "VENDA", BigDecimal.valueOf(12.5));
        Registro ajuste = registro(2L, "AJUSTE", BigDecimal.valueOf(100));
        Registro vendaSemValor = registro(1L, "VENDA", null);
        when(repository.buscarVendasPorMesEAno(10, 2026)).thenReturn(List.of(venda, ajuste, vendaSemValor));

        var resposta = service.obterHistorico(10, 2026);

        assertThat(resposta.getTotalDoMes()).isEqualByComparingTo("12.50");
        assertThat(resposta.getLancamentos()).extracting(item -> item.id()).containsExactly(3L, 2L, 1L);
        assertThat(resposta.getLancamentos().getFirst().produto().id()).isEqualTo(1L);
        verify(repository).buscarVendasPorMesEAno(10, 2026);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void consultaSemPeriodoPreservaContratoDeTodosOsRegistros() {
        when(repository.findAllByOrderByDataVendaDescIdDesc()).thenReturn(List.of());
        var resposta = service.obterHistorico(0, 0);
        assertThat(resposta.getTotalDoMes()).isZero();
        assertThat(resposta.getLancamentos()).isEmpty();
        verify(repository).findAllByOrderByDataVendaDescIdDesc();
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"0,2026", "1,0", "13,2026", "-1,2026", "1,-1", "1,10000", "0,10000"})
    void rejeitaPeriodoInvalidoSemConsultarBanco(int mes, int ano) {
        assertThatThrownBy(() -> service.obterHistorico(mes, ano))
                .isInstanceOfSatisfying(ApiException.class, erro -> assertThat(erro.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "12,9999"})
    void preservaLimitesDePeriodoAceitos(int mes, int ano) {
        when(repository.buscarVendasPorMesEAno(mes, ano)).thenReturn(List.of());
        assertThat(service.obterHistorico(mes, ano).getLancamentos()).isEmpty();
        verify(repository).buscarVendasPorMesEAno(mes, ano);
    }

    private Registro registro(Long id, String acao, BigDecimal total) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto de teste");
        produto.setQuantidade(10);
        produto.setPreco(12.5);
        Registro registro = new Registro(produto, 1, total, null, acao);
        registro.setId(id);
        return registro;
    }
}
