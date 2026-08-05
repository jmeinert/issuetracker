package de.jmeinert.issuetracker;

import de.jmeinert.issuetracker.config.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IssuetrackerApplicationTests {

    @Test
    void contextLoads() {
    }

}
