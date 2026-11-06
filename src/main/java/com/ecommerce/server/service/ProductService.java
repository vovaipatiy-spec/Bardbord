package com.ecommerce.server.service;

import com.ecommerce.server.dto.product.ProductPatchRequest;
import com.ecommerce.server.dto.product.ProductResponse;
import com.ecommerce.server.entity.Category;
import com.ecommerce.server.entity.Product;
import com.ecommerce.server.repository.CategoryRepository;
import com.ecommerce.server.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponse> findAll() {
        return productRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long id, ProductPatchRequest request) {
        Product product = findById(id);

        if (request.name() != null) {
            String name = request.name().trim();

            if (name.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product name must not be blank"
                );
            }

            if (!name.equals(product.getName())
                    && productRepository.existsByNameAndIdNot(name, id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A product with this name already exists"
                );
            }

            product.setName(name);
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.stock() != null) {
            product.setStock(request.stock());
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Category not found"
                    ));

            product.setCategory(category);
        }

        return toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        productRepository.delete(findById(id));
    }

    private Product findById(Long id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }

    private ProductResponse toResponse(Product product) {
        Long categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        String categoryName = product.getCategory() == null ? null : product.getCategory().getName();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                categoryId,
                categoryName,
                product.getCreatedAt()
        );
    }
}
