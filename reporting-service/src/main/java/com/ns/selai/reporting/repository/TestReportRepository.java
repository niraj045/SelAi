package com.ns.selai.reporting.repository;

import com.ns.selai.reporting.model.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    Optional<TestReport> findByTestRunId(Long testRunId);

    List<TestReport> findAllByOrderByGeneratedAtDesc();
}
