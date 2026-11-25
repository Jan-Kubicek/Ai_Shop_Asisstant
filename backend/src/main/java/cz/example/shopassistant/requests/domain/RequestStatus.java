package cz.example.shopassistant.requests.domain;

public enum RequestStatus {
    CREATED,
    VALIDATING,
    PROCESSING,
    REVIEW,
    QUEUED_FOR_SHOP,
    IMPORTED,
    FAILED
}
