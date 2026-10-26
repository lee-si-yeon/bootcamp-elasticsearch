package com.example.elasticsearch.product.service;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.example.elasticsearch.product.document.ProductDocument;
import com.example.elasticsearch.product.entity.Product;
import com.example.elasticsearch.product.entity.ProductKeyword;
import com.example.elasticsearch.product.repository.ProductDocumentRepository;
import com.example.elasticsearch.product.repository.ProductRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Vector;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public Product create(String name, List<String> keywords) {
        Product product = new Product();
        product.setName(name);
        keywords.forEach(product::addKeyword);
        float[] embedding = embed(keywords);
        product.setEmbedding(embedding); // pgvector 컬럼에도 저장 (13강에서 Elasticsearch를 대체)
        productRepository.save(product);

        saveDocument(product.getId(), embedding);
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
        float[] embedding = embed(keywords);
        product.setEmbedding(embedding);

        saveDocument(id, embedding);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
        productDocumentRepository.deleteById(id);
    }

    // 검색어를 임베딩해서, 벡터가 가장 가까운 상품 k개를 유사도 순으로 돌려줌
    public List<Product> knnSearch(String query, int k) {
        float[] queryVector = embeddingModel.embed(query);
        return findProductsByVector(queryVector, k);
    }

    // 여러 키워드를 각각 임베딩한 뒤 평균 벡터로 검색함 (상품 벡터를 만드는 방식과 같음)
    public List<Product> knnSearch(List<String> keywords, int k) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        float[] queryVector = calculateAverage(embeddingModel.embed(keywords));
        return findProductsByVector(queryVector, k);
    }

    // 기준 상품의 키워드로 검색하고, 1위로 나오는 기준 상품 자신은 뺌
    // (Elasticsearch 9.x는 저장한 벡터를 _source로 돌려주지 않으므로 키워드로 다시 임베딩함)
    @Transactional(readOnly = true)
    public List<Product> findSimilarProducts(Long productId, int k) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        List<String> keywords = product.getKeywords().stream()
                .map(ProductKeyword::getKeyword)
                .toList();

        return knnSearch(keywords, k + 1).stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(k)
                .toList();
    }

    private List<Product> findProductsByVector(float[] queryVector, int k) {
        List<Long> ids = knnSearchByVector(queryVector, k, k * 2).stream()
                .map(ProductDocument::getId)
                .toList();

        // findAllById는 순서를 보장하지 않음. KNN이 돌려준 id 순서(유사도 순)대로 다시 정렬함
        Map<Long, Product> productMap = StreamSupport
                .stream(productRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ProductDocument> knnSearchByVector(float[] queryVector, int k, int numCandidates) {
        KnnSearch knnSearch = KnnSearch.of(knn -> knn
                .field("embedding")
                .queryVector(toFloatList(queryVector)) // Elasticsearch 클라이언트는 List<Float>를 받음
                .k(k)                                  // 최종으로 돌려받을 개수
                .numCandidates(numCandidates)          // 샤드마다 살펴볼 후보 수 (클수록 정확, 느림)
        );

        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(knnSearch)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchTemplate.search(query, ProductDocument.class);
        searchHits.getSearchHits().forEach(hit -> System.out.println("  KNN hit id=" + hit.getContent().getId() + " score=" + hit.getScore()));

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    // 키워드가 없으면 벡터도 없음 (검색 대상에서 빠짐)
    private float[] embed(List<String> keywords) {
        return keywords.isEmpty() ? null : calculateAverage(embeddingModel.embed(keywords));
    }

    private void saveDocument(Long productId, float[] embedding) {
        ProductDocument document = new ProductDocument();
        document.setId(productId);

        document.setEmbedding(embedding);
        productDocumentRepository.save(document);
    }

    // pgvector로 검색 : 코사인 거리가 가까운 순서로 k개
    public List<Product> vectorSearch(String query, int k) {
        float[] queryVector = embeddingModel.embed(query);
        SearchResults<Product> results = productRepository.searchByEmbeddingNear(
                Vector.of(queryVector),
                // cosine()의 Score는 "코사인 거리(embedding <=> 쿼리)의 상한"임. 거리는 0(같음) ~ 2(반대)
                // 2.0이면 거르지 않고 가까운 순서로 k개를 받음. 0.5처럼 주면 그보다 먼 상품은 빠짐
                Score.of(2.0, ScoringFunction.cosine()),
                Limit.of(k));
        results.forEach(r -> System.out.println("  pgvector hit " + r.getContent().getName() + " distance=" + r.getScore().getValue()));
        return results.stream().map(SearchResult::getContent).toList();
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
