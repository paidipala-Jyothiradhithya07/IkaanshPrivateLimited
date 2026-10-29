package com.ikaanshpl.finance.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ikaanshpl.finance.entity.Category;



public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByName(Long userId);
    List<Category> findByUserId(Long userId);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}