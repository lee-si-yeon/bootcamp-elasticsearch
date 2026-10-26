package com.example.elasticsearch.product;

import com.example.elasticsearch.TestcontainersConfiguration;
import com.example.elasticsearch.product.entity.Product;
import com.example.elasticsearch.product.repository.ProductRepository;
import com.example.elasticsearch.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductSearchTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        productService.create("Gaming Laptop", List.of("gaming", "laptop", "graphics-card"));
        productService.create("Business Laptop", List.of("office", "document", "lightweight"));
        productService.create("Gaming Mouse", List.of("gaming", "mouse", "RGB"));
        productService.create("Espresso Coffee", List.of("coffee", "espresso", "beverage"));
    }

    @Test
    @DisplayName("KNN 검색 - 'gaming'과 가장 가까운 상품은 게이밍 상품이다")
    void t1() {
        List<Product> results = productService.knnSearch("gaming", 2);

        System.out.println("gaming 검색 결과 : " + results.stream().map(Product::getName).toList());
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(product -> product.getName().startsWith("Gaming")));
    }

    @Test
    @DisplayName("KNN 검색 - 키워드에 없는 단어도 의미로 찾는다")
    void t2() {
        List<Product> results = productService.knnSearch("hot drink in the morning", 1);

        System.out.println("hot drink 검색 결과 : " + results.stream().map(Product::getName).toList());
        assertEquals("Espresso Coffee", results.get(0).getName());
    }

    @Test
    @DisplayName("KNN 검색 - k개만 돌려준다")
    void t3() {
        assertEquals(3, productService.knnSearch("computer", 3).size());
    }

    @Test
    @DisplayName("키워드 목록 검색 - 여러 키워드의 평균 벡터로 찾는다")
    void t4() {
        List<Product> results = productService.knnSearch(List.of("coffee", "espresso"), 1);

        assertEquals("Espresso Coffee", results.get(0).getName());
    }

    @Test
    @DisplayName("유사 상품 - 기준 상품 자신은 빠지고, 가장 비슷한 상품이 1위다")
    void t5() {
        Product gamingLaptop = productRepository.findAll().iterator().next();

        List<Product> similar = productService.findSimilarProducts(gamingLaptop.getId(), 2);

        System.out.println("Gaming Laptop 유사 상품 : " + similar.stream().map(Product::getName).toList());
        assertEquals(2, similar.size());
        assertTrue(similar.stream().noneMatch(p -> p.getId().equals(gamingLaptop.getId())));
        assertEquals("Gaming Mouse", similar.get(0).getName());
    }
}
