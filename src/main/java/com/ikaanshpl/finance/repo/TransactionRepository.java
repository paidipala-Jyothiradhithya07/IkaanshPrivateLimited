package com.ikaanshpl.finance.repo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ikaanshpl.finance.entity.Transaction;


public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Fetch account + category in the same query so listing a page is 1 query, not 1 + 2N. */
    @Override
    @EntityGraph(attributePaths = {"account", "category"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
