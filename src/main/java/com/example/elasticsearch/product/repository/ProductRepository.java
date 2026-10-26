package com.example.elasticsearch.product.repository;

import com.example.elasticsearch.product.entity.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
