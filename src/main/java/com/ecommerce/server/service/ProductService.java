package com.ecommerce.server.service;

import com.ecommerce.server.config.CacheConfig;
import com.ecommerce.server.dto.product.ProductPatchRequest;
import com.ecommerce.server.dto.product.ProductResponse;
import com.ecommerce.server.dto.product.ProductSearchRequest;
import com.ecommerce.server.dto.product.SortDirection;
import com.ecommerce.server.entity.Category;
import com.ecommerce.server.entity.Product;
import com.ecommerce.server.repository.CategoryRepository;
import com.ecommerce.server.repository.ProductRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

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

    @Cacheable(cacheNames = CacheConfig.PRODUCT_SEARCH_CACHE, key = "#request")
    @Transactional(readOnly = true)
    public List<ProductResponse> search(ProductSearchRequest request) {
        String query = request.query() == null ? null : request.query().trim();

        if (query != null && query.isEmpty()) {
            query = null;
        }

        if (request.minPrice() != null
                && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minPrice must not be greater than maxPrice"
            );
        }

        if (request.minStock() != null
                && request.maxStock() != null
                && request.minStock() > request.maxStock()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minStock must not be greater than maxStock"
            );
        }

        return productRepository
                .search(
                        query,
                        request.categoryId(),
                        request.minPrice(),
                        request.maxPrice(),
                        request.minStock(),
                        request.maxStock(),
                        resolveSort(request)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Sort resolveSort(ProductSearchRequest request) {
        String requested = request.sortBy() == null
                ? ""
                : request.sortBy().trim().toLowerCase(Locale.ROOT)
                        .replace("_", "")
                        .replace("-", "");

        String property = switch (requested) {
            case "", "id" -> "id";
            case "name" -> "name";
            case "price" -> "price";
            case "stock" -> "stock";
            case "createdat", "date" -> "createdAt";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sortBy: " + request.sortBy()
                            + ". Allowed values: id, name, price, stock, createdAt"
            );
        };

        Sort.Direction direction = request.sortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_SEARCH_CACHE, allEntries = true)
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

    @CacheEvict(cacheNames = CacheConfig.PRODUCT_SEARCH_CACHE, allEntries = true)
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
