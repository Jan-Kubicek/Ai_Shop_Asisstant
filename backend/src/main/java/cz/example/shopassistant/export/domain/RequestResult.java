package cz.example.shopassistant.export.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "results")
public class RequestResult {

    @Id
    private String id;

    @Indexed(unique = true)
    private String requestId;

    private String webTextJson;

    private String socialTextJson;

    private String wooJson;

    private String marketingTextJson;

    private String productImageUrl;

    private String modelImageUrl;

    private String modelMediaId;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getWebTextJson() { return webTextJson; }
    public void setWebTextJson(String webTextJson) { this.webTextJson = webTextJson; }
    public String getSocialTextJson() { return socialTextJson; }
    public void setSocialTextJson(String socialTextJson) { this.socialTextJson = socialTextJson; }
    public String getWooJson() { return wooJson; }
    public void setWooJson(String wooJson) { this.wooJson = wooJson; }
    public String getMarketingTextJson() { return marketingTextJson; }
    public void setMarketingTextJson(String marketingTextJson) { this.marketingTextJson = marketingTextJson; }
    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
    public String getModelMediaId() { return modelMediaId; }
    public void setModelMediaId(String modelMediaId) { this.modelMediaId = modelMediaId; }
    public String getModelImageUrl() { return modelImageUrl; }
    public void setModelImageUrl(String modelImageUrl) { this.modelImageUrl = modelImageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
