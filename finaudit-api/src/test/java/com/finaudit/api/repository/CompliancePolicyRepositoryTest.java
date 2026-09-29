package com.finaudit.api.repository;

import com.finaudit.api.entity.CompliancePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CompliancePolicyRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CompliancePolicyRepository policyRepository;

    @Test
    @DisplayName("Should save and find compliance policies by category and title")
    void shouldSaveAndQueryPolicies() {
        CompliancePolicy policy = new CompliancePolicy(
                "Travel Expense Policy 2024",
                "Meals capped at $75 per day. Airfare requires VP pre-approval.",
                "TRAVEL",
                LocalDate.of(2024, 1, 1)
        );
        policyRepository.save(policy);

        List<CompliancePolicy> travelPolicies = policyRepository.findByCategory("TRAVEL");
        assertThat(travelPolicies).isNotEmpty();
        assertThat(travelPolicies).anyMatch(p -> "Travel Expense Policy 2024".equals(p.getTitle()));

        Optional<CompliancePolicy> found = policyRepository.findByTitle("Travel Expense Policy 2024");
        assertThat(found).isPresent();
    }
}
