package com.example.elasticsearch.product.service;

import com.example.elasticsearch.product.document.ProductDocument;
import com.example.elasticsearch.product.entity.Product;
import com.example.elasticsearch.product.repository.ProductDocumentRepository;
import com.example.elasticsearch.product.repository.ProductRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    public Product create(String name, List<String> keywords) {
        Product product = new Product();
        product.setName(name);
        keywords.forEach(product::addKeyword);
        productRepository.save(product);

        saveDocument(product.getId(), keywords);
        return product;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false).toList();
    }

    public Optional<ProductDocument> findDocumentById(Long id) {
        return productDocumentRepository.findById(id);
    }

    @Transactional
    public Product update(Long id, String name, List<String> keywords) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(name);
        product.getKeywords().clear();
        keywords.forEach(product::addKeyword);

        saveDocument(id, keywords);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
        productDocumentRepository.deleteById(id);
    }

    private void saveDocument(Long productId, List<String> keywords) {
        ProductDocument document = new ProductDocument();
        document.setId(productId);

        if (!keywords.isEmpty()) {
            document.setEmbedding(calculateAverage(embeddingModel.embed(keywords)));
        }
        productDocumentRepository.save(document);
    }

    private float[] calculateAverage(List<float[]> vectors) {
        int dimension = vectors.get(0).length;
        float[] sum = new float[dimension];

        for (float[] vector : vectors) {
            for (int i = 0; i < dimension; i++) {
                sum[i] += vector[i];
            }
        }

        for (int i = 0; i < dimension; i++) {
            sum[i] /= vectors.size();
        }

        return sum;
    }
}
