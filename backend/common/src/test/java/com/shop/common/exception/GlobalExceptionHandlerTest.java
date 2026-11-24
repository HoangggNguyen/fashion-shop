package com.shop.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessReturnsMappedStatusAndErrorCode() {
        BusinessException ex = new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product 99 not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("Product 99 not found");
    }

    @Test
    void handleUnknownHidesInternalStackTrace() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("secret db error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("Internal server error");
    }
}
