package com.finaudit.api.repository;

import com.finaudit.api.entity.Report;
import com.finaudit.core.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    List<Report> findByOwnerId(Long ownerId);
    List<Report> findByStatus(ReportStatus status);
}
