package com.example.elasticsearch.product;

import com.example.elasticsearch.TestcontainersConfiguration;
import com.example.elasticsearch.product.document.ProductDocument;
import com.example.elasticsearch.product.repository.ProductDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductDocumentTests {

    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("products 인덱스의 embedding 필드는 384차원 dense_vector다")
    @SuppressWarnings("unchecked")
    void t1() {
        Map<String, Object> mapping = elasticsearchOperations.indexOps(ProductDocument.class).getMapping();
        Map<String, Object> embedding = (Map<String, Object>) ((Map<String, Object>) mapping.get("properties")).get("embedding");
        System.out.println("embedding 매핑 : " + embedding);

        assertEquals("dense_vector", embedding.get("type"));
        assertEquals(384, embedding.get("dims"));
        assertEquals("cosine", embedding.get("similarity"));
    }

    @Test
    @DisplayName("ProductDocument 저장 후 id로 조회")
    void t2() {
        ProductDocument document = new ProductDocument();
        document.setId(9999L);
        document.setEmbedding(new float[384]);
        document.getEmbedding()[0] = 1.0f;

        productDocumentRepository.save(document);

        assertTrue(productDocumentRepository.findById(9999L).isPresent());
        productDocumentRepository.deleteById(9999L);
    }
}
