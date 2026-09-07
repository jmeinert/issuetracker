package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_id_seq")
    @SequenceGenerator(
        name = "issue_id_seq",
        sequenceName = "issue_id_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuePriority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected Issue() {
    }

    public Issue(
      String title,
      String description,
      IssueStatus status,
      IssuePriority priority,
      Project project,
      User reporter,
      User assignee
    ) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.project = project;
        this.reporter = reporter;
        this.assignee = assignee;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public Project getProject() {
        return project;
    }

    public User getReporter() {
        return reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateDetails(String title, String description, IssuePriority priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    void changeStatusTo(IssueStatus status) {
        this.status = status;
    }

    void changeAssigneeTo(User assignee) {
        this.assignee = assignee;
    }
}
