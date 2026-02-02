package com.ns.selai.testmanagement.repository;

import com.ns.selai.testmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find all active projects
    List<Project> findByIsActiveTrue();

    // Find project by name
    Optional<Project> findByName(String name);

    // Find projects by created by
    List<Project> findByCreatedBy(String createdBy);

    // Search projects by name (case-insensitive)
    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Project> searchByName(String searchTerm);

    // Find projects by test type
    List<Project> findByTestType(String testType);
}
