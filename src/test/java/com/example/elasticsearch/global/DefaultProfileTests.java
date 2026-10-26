package com.example.elasticsearch.global;

import com.example.elasticsearch.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "OPENAI_API_KEY=sk-test-dummy")
class DefaultProfileTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Map<String, EmbeddingModel> embeddingModels;

    @Test
    @DisplayName("default 프로필 : 임베딩은 OpenAI 하나, ChatClient는 openaiChatClient")
    void t1() {
        assertEquals(1, embeddingModels.size());
        assertInstanceOf(OpenAiEmbeddingModel.class, embeddingModels.values().iterator().next());
        assertTrue(context.containsBean("openaiChatClient"));
        assertFalse(context.containsBean("ollamaChatClient"));
        assertNotNull(context.getBean(ChatClient.class));
    }
}
