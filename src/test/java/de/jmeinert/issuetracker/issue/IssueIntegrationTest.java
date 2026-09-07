package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.auth.LoginResponse;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IssueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    private Project project;

    private User reporter;

    @BeforeEach
    void setUp() {
        project = new Project("TestName", "TestDescription");
        projectRepository.saveAndFlush(project);

        reporter = new UserTestBuilder()
            .username("reporter")
            .email("reporter@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .enabled(true)
            .build();
        userRepository.saveAndFlush(reporter);
    }

    @Test
    void create_persistsIssueWithAuthenticatedUserAsReporter() throws Exception {
        String token = login("reporter", "password");

        mockMvc.perform(post("/api/projects/{projectId}/issues", project.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reporterId").value(reporter.getId()));

        entityManager.flush();
        entityManager.clear();

        var issues = issueRepository.findAll();
        assertEquals(1, issues.size());

        Issue issue = issues.getFirst();

        assertEquals("TestTitle", issue.getTitle());
        assertEquals("TestDescription", issue.getDescription());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertEquals(IssuePriority.LOW, issue.getPriority());
        assertEquals(project.getId(), issue.getProject().getId());
        assertEquals(reporter.getId(), issue.getReporter().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignmentLifecycle_adminAssignsReassignsAndUnassignsIssue() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        Long issueId = issue.getId();

        User assignee1 = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .enabled(true)
            .build();
        User assignee2 = new UserTestBuilder()
            .username("assignee2")
            .email("assignee2@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .enabled(true)
            .build();
        userRepository.saveAllAndFlush(List.of(assignee1, assignee2));

        entityManager.clear();
        Issue persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertNull(persistedIssue.getAssignee());

        // Assign issue
        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assignee1.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(assignee1.getId()));

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertEquals(assignee1.getId(), persistedIssue.getAssignee().getId());

        // Reassign issue
        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assignee2.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(assignee2.getId()));

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertEquals(assignee2.getId(), persistedIssue.getAssignee().getId());

        // Unassign issue
        mockMvc.perform(delete("/api/issues/{issueId}/assignee", issueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").isEmpty());

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertNull(persistedIssue.getAssignee());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        LoginResponse loginResponse = jsonMapper.readValue(response, LoginResponse.class);
        return loginResponse.token();
    }
}
