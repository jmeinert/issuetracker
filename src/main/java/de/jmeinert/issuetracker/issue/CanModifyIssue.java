package de.jmeinert.issuetracker.issue;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('ADMIN') or @issueAuthorization.canModify(#issueId)")
public @interface CanModifyIssue {
}
