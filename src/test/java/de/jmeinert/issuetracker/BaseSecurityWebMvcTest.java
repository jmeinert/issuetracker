package de.jmeinert.issuetracker;

import de.jmeinert.issuetracker.error.SecurityExceptionHandler;
import de.jmeinert.issuetracker.security.SecurityConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    SecurityExceptionHandler.class
})
public abstract class BaseSecurityWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;
}
