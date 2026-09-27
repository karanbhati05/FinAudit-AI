package com.finaudit.api.repository;

import com.finaudit.api.entity.CompliancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompliancePolicyRepository extends JpaRepository<CompliancePolicy, Long> {
    List<CompliancePolicy> findByCategory(String category);
    Optional<CompliancePolicy> findByTitle(String title);
}
