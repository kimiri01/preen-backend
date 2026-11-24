package com.preen.preen_backend.accommodation.repository;

import com.preen.preen_backend.accommodation.domain.RefundPolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundPolicyRuleRepository extends JpaRepository<RefundPolicyRule, Long> {

    List<RefundPolicyRule> findByRefundPolicyIdOrderByDaysBeforeDesc(Long refundPolicyId);
}
