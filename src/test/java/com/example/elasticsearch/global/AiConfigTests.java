package com.example.elasticsearch.global;

import com.example.elasticsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class AiConfigTests {

    @Autowired
    private Map<String, EmbeddingModel> embeddingModels;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatClient chatClient;

    @Test
    @DisplayName("test 프로필에는 Ollama EmbeddingModel 하나와 ChatClient가 등록된다")
    void t1() {
        assertEquals(1, embeddingModels.size());
        assertTrue(embeddingModels.containsKey("ollamaEmbeddingModel"));
        assertNotNull(chatClient);
    }

    @Test
    @DisplayName("임베딩 - 텍스트 하나가 384차원 벡터가 된다")
    void t2() {
        float[] vector = embeddingModel.embed("gaming laptop");

        assertEquals(384, vector.length);
        System.out.println("gaming laptop -> [" + vector[0] + ", " + vector[1] + ", " + vector[2] + ", ...] (" + vector.length + "차원)");
    }

    @Test
    @DisplayName("임베딩 - 의미가 비슷한 단어끼리 코사인 유사도가 더 높다")
    void t3() {
        List<float[]> vectors = embeddingModel.embed(List.of("laptop", "notebook computer", "coffee"));

        double laptopNotebook = cosine(vectors.get(0), vectors.get(1));
        double laptopCoffee = cosine(vectors.get(0), vectors.get(2));
        System.out.printf("laptop-notebook computer : %.3f, laptop-coffee : %.3f%n", laptopNotebook, laptopCoffee);
        assertTrue(laptopNotebook > laptopCoffee);
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
