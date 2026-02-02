package com.ns.selai.orchestration.repository;

import com.ns.selai.orchestration.model.TestRun;
import com.ns.selai.orchestration.model.TestRun.TestRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // Find test runs by project ID
    List<TestRun> findByProjectId(Long projectId);

    // Find test runs by status
    List<TestRun> findByStatus(TestRunStatus status);

    // Find test runs by project and status
    List<TestRun> findByProjectIdAndStatus(Long projectId, TestRunStatus status);

    // Find recent test runs for a project (ordered by started_at desc)
    List<TestRun> findByProjectIdOrderByStartedAtDesc(Long projectId);
}
