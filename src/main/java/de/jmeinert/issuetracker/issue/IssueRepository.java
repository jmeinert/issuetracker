package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Page<Issue> findAllByProject(Project project, Pageable pageable);

    boolean existsByProject(Project project);
}
