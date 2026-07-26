package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findAllByProject(Project project);

    boolean existsByProject(Project project);
}
