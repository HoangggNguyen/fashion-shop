package com.shop.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    EMAIL_EXISTS(HttpStatus.CONFLICT, "Email already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    SKU_EXISTS(HttpStatus.CONFLICT, "SKU already exists"),
    STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "Insufficient stock"),
    CART_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Cart is empty"),
    ITEM_UNAVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "One or more items are unavailable"),
    COUPON_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is invalid"),
    ORDER_INVALID_STATE(HttpStatus.CONFLICT, "Invalid order state transition"),
    PAYMENT_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "Invalid payment signature"),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "Upstream service error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
