package com.shop.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ApiResponseTest {

    @Test
    void okResponseHasSuccessTrueAndData() {
        ApiResponse<String> response = ApiResponse.ok("hello");
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.error()).isNull();
    }

    @Test
    void errorResponseHasSuccessFalseAndError() {
        ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Item missing", List.of("id=1"));
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("NOT_FOUND");
        assertThat(response.error().message()).isEqualTo("Item missing");
        assertThat(response.error().details()).containsExactly("id=1");
    }

    @Test
    void pageResponseMapsSpringPage() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);
        PageResponse<String> result = PageResponse.from(page);
        assertThat(result.items()).containsExactly("a", "b");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalItems()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }
}
