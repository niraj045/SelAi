package com.ns.selai.execution.repository;

import com.ns.selai.execution.model.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    // Find all test executions for a given test run ID
    List<TestExecution> findByTestRunId(Long testRunId);
}