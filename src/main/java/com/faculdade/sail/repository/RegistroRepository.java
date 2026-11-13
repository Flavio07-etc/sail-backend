package com.faculdade.sail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;


import com.faculdade.sail.model.Registro;

@Repository
public interface RegistroRepository extends JpaRepository<Registro, Long> {

    @EntityGraph(attributePaths = {"usuario", "produto", "produto.criadoPor", "produto.atualizadoPor"})
    @Query("SELECT r FROM Registro r WHERE MONTH(r.dataVenda) = :mes AND YEAR(r.dataVenda) = :ano ORDER BY r.dataVenda DESC, r.id DESC")
    List<Registro> buscarVendasPorMesEAno(@Param("mes") int mes, @Param("ano") int ano);

    @EntityGraph(attributePaths = {"usuario", "produto", "produto.criadoPor", "produto.atualizadoPor"})
    List<Registro> findAllByOrderByDataVendaDescIdDesc();

    Optional<Registro> findByUsuarioIdAndChaveOperacao(Long usuarioId, String chaveOperacao);

    @EntityGraph(attributePaths = {"usuario", "produto", "produto.criadoPor", "produto.atualizadoPor"})
    @Query("SELECT r FROM Registro r WHERE (:mes = 0 OR (MONTH(r.dataVenda) = :mes AND YEAR(r.dataVenda) = :ano)) "
            + "AND (:dataCursor IS NULL OR r.dataVenda < :dataCursor OR (r.dataVenda = :dataCursor AND r.id < :idCursor)) "
            + "ORDER BY r.dataVenda DESC, r.id DESC")
    List<Registro> buscarPagina(@Param("mes") int mes, @Param("ano") int ano,
            @Param("dataCursor") LocalDateTime dataCursor, @Param("idCursor") Long idCursor, Pageable limite);

    @Query(value = "SELECT COALESCE(SUM(valor_total), 0) FROM registros WHERE CAST(acao AS BINARY) = CAST('VENDA' AS BINARY) "
            + "AND (:mes = 0 OR (MONTH(data_venda) = :mes AND YEAR(data_venda) = :ano))", nativeQuery = true)
    BigDecimal totalizarVendas(@Param("mes") int mes, @Param("ano") int ano);
}
