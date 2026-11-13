package com.faculdade.sail.repository;

import com.faculdade.sail.model.Produto;
import com.faculdade.sail.dtos.ResumoEstoque;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = {"criadoPor", "atualizadoPor"})
    List<Produto> findAllByAtivoTrueOrderByIdDesc();

    @EntityGraph(attributePaths = {"criadoPor", "atualizadoPor"})
    List<Produto> findAllByAtivoTrueOrderByIdDesc(Pageable limite);

    @EntityGraph(attributePaths = {"criadoPor", "atualizadoPor"})
    List<Produto> findAllByAtivoTrueAndIdLessThanOrderByIdDesc(Long id, Pageable limite);

    @Query("SELECT new com.faculdade.sail.dtos.ResumoEstoque(COUNT(p), COALESCE(SUM(p.preco * p.quantidade), 0.0), "
            + "COALESCE(SUM(CASE WHEN p.quantidade <= 0 THEN 1L ELSE 0L END), 0L)) FROM Produto p WHERE p.ativo = true")
    ResumoEstoque resumirEstoque();

    @EntityGraph(attributePaths = {"criadoPor", "atualizadoPor"})
    Optional<Produto> findByIdAndAtivoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.id = :id")
    Optional<Produto> buscarParaAtualizacao(@Param("id") Long id);

}
