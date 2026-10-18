package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Page<Issue> findAllByProject(Project project, Pageable pageable);

    boolean existsByProject(Project project);

    @Query("""
        SELECT CASE WHEN COUNT(issue) > 0 THEN true ELSE false END
        FROM Issue issue
        WHERE issue.id = :issueId
            AND (
                issue.reporter.id = :userId
                OR issue.assignee.id = :userId
            )
        """)
    boolean existsByIdAndParticipantId(@Param("issueId") Long issueId, @Param("userId") Long userId);
}
