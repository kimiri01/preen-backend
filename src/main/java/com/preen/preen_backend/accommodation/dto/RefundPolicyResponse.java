package com.preen.preen_backend.accommodation.dto;


import java.util.List;

public record RefundPolicyResponse(Long id, String version, String policyText, List<RefundRule> rules) {
}