package com.example.elasticsearch.product;

import com.example.elasticsearch.TestcontainersConfiguration;
import com.example.elasticsearch.product.service.ProductChatService;
import com.example.elasticsearch.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
public class ProductChatServiceTests {

    @Autowired
    private ProductChatService productChatService;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("chatStream - 답변이 여러 토큰으로 나뉘어 온다")
    void t1() {
        productService.create("Espresso Coffee", List.of("coffee", "espresso", "beverage"));

        List<String> tokens = productChatService.chatStream("Recommend a hot drink").collectList().block();

        assertNotNull(tokens);
        assertTrue(tokens.size() > 1);
        System.out.println("토큰 수 : " + tokens.size() + ", 답변 : " + String.join("", tokens));
    }
}
