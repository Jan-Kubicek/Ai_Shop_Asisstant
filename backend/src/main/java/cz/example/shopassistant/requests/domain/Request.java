package cz.example.shopassistant.requests.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "requests")
public class Request {

    @Id
    private String id;

    private String userId;

    private String description;

    private RequestOptions options;

    private RequestStatus status = RequestStatus.CREATED;

    private String errorMessage;

    private String mediaId;

    private java.util.List<SizePrice> sizePrices;

    private Instant createdAt;

    private Instant finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RequestOptions getOptions() { return options; }
    public void setOptions(RequestOptions options) { this.options = options; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public java.util.List<SizePrice> getSizePrices() { return sizePrices; }
    public void setSizePrices(java.util.List<SizePrice> sizePrices) { this.sizePrices = sizePrices; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public static class SizePrice {
        private int size;
        private java.math.BigDecimal price;

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
    }

    public static class RequestOptions {
        private boolean generateWebText;
        private boolean generateSocialPost;
        private boolean generateModelPhoto;
        private String modelDescription;
        private String sizeFrom;
        private String sizeTo;
        private java.math.BigDecimal basePrice;
        private java.math.BigDecimal priceIncreasePercent;

        public String getModelDescription() { return modelDescription; }
        public void setModelDescription(String modelDescription) { this.modelDescription = modelDescription; }
        public String getSizeFrom() { return sizeFrom; }
        public void setSizeFrom(String sizeFrom) { this.sizeFrom = sizeFrom; }
        public String getSizeTo() { return sizeTo; }
        public void setSizeTo(String sizeTo) { this.sizeTo = sizeTo; }
        public java.math.BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(java.math.BigDecimal basePrice) { this.basePrice = basePrice; }
        public java.math.BigDecimal getPriceIncreasePercent() { return priceIncreasePercent; }
        public void setPriceIncreasePercent(java.math.BigDecimal priceIncreasePercent) { this.priceIncreasePercent = priceIncreasePercent; }

        public boolean isGenerateWebText() { return generateWebText; }
        public void setGenerateWebText(boolean generateWebText) { this.generateWebText = generateWebText; }
        public boolean isGenerateSocialPost() { return generateSocialPost; }
        public void setGenerateSocialPost(boolean generateSocialPost) { this.generateSocialPost = generateSocialPost; }
        public boolean isGenerateModelPhoto() { return generateModelPhoto; }
        public void setGenerateModelPhoto(boolean generateModelPhoto) { this.generateModelPhoto = generateModelPhoto; }
    }
}
