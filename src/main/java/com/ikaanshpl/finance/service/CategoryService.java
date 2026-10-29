package com.ikaanshpl.finance.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.CategoryResponse;
import com.ikaanshpl.finance.dto.Dtos.CreateCategoryRequest;
import com.ikaanshpl.finance.entity.Category;
import com.ikaanshpl.finance.repo.CategoryRepository;


@Service
public class CategoryService {
	 private final CategoryRepository categories;
	 

	    public CategoryService(CategoryRepository categories) {
		super();
		this.categories = categories;
	}

		@Transactional
	    public CategoryResponse create(Long userId, CreateCategoryRequest r) {
	        String name = r.name().trim();
	        if (categories.existsByUserIdAndNameIgnoreCase(userId, name)) {
	            throw new ApiException(HttpStatus.CONFLICT, "Category already exists");
	        }
	        return CategoryResponse.from(categories.save(new Category(userId, name)));
	    }

	    @Transactional(readOnly = true)
	    public List<CategoryResponse> list(Long userId) {
	        return categories.findByUserIdOrderByName(userId).stream().map(CategoryResponse::from).toList();
	    }
}
