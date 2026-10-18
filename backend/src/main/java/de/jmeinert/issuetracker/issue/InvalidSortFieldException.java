package de.jmeinert.issuetracker.issue;

import java.util.List;

public class InvalidSortFieldException extends RuntimeException {

    public InvalidSortFieldException(String sortField, List<String> allowedSortFields) {
        super(
            "Sort field '%s' is invalid. Allowed sort fields: %s."
                .formatted(
                    sortField,
                    String.join(", ", allowedSortFields)
                )
        );
    }
}
