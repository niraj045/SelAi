package com.ns.selai.testmanagement.controller;

import com.ns.selai.testmanagement.dto.ProjectDTO;
import com.ns.selai.testmanagement.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure properly in production
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project
     * POST /api/projects
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO projectDTO) {
        log.info("REST request to create project: {}", projectDTO.getName());
        ProjectDTO createdProject = projectService.createProject(projectDTO);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    /**
     * Get all active projects
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        log.info("REST request to get all projects");
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        log.info("REST request to get project by ID: {}", id);
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Update project
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDTO projectDTO) {
        log.info("REST request to update project: {}", id);
        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete project (soft delete)
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("REST request to delete project: {}", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search projects by name
     * GET /api/projects/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProjectDTO>> searchProjects(@RequestParam String q) {
        log.info("REST request to search projects with term: {}", q);
        List<ProjectDTO> projects = projectService.searchProjects(q);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by test type
     * GET /api/projects/by-type/{testType}
     */
    @GetMapping("/by-type/{testType}")
    public ResponseEntity<List<ProjectDTO>> getProjectsByTestType(@PathVariable String testType) {
        log.info("REST request to get projects by test type: {}", testType);
        List<ProjectDTO> projects = projectService.getProjectsByTestType(testType);
        return ResponseEntity.ok(projects);
    }
}
