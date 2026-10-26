package com.example.elasticsearch.product.controller;

import com.example.elasticsearch.product.entity.Product;
import com.example.elasticsearch.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody CreateRequest request) {
        return ResponseEntity.ok(productService.create(request.name(), request.keywords()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.update(id, request.name(), request.keywords()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String query, @RequestParam(defaultValue = "5") int k) {
        return ResponseEntity.ok(productService.knnSearch(query, k));
    }

    public record CreateRequest(String name, List<String> keywords) {}
    public record UpdateRequest(String name, List<String> keywords) {}
}
