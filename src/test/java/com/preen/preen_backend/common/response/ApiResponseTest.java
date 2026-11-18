package com.preen.preen_backend.common.response;

import com.preen.preen_backend.auth.dto.SignupResponse;
import com.preen.preen_backend.common.exception.CommonErrorCode;
import com.preen.preen_backend.common.security.UserRole;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void okSerializesSignupResultInsideData() throws Exception {
        var response = ApiResponse.ok(new SignupResponse(1L, UserRole.GUEST, "ACTIVE"));
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("userId").asLong()).isEqualTo(1L);
        assertThat(json.get("data").get("role").asText()).isEqualTo("GUEST");
        assertThat(json.get("code").asText()).isEqualTo("OK");
    }

    @Test
    void failurePreservesErrorCodeAndMessage() throws Exception {
        var error = CommonErrorCode.INVALID_INPUT;
        var json = mapper.readTree(mapper.writeValueAsString(ApiResponse.failure(error)));
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.get("code").asText()).isEqualTo(error.name());
        assertThat(json.get("message").asText()).isEqualTo(error.message());
    }
}
