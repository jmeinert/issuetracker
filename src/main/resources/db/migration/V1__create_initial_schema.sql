CREATE SEQUENCE project_id_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE issue_id_seq START WITH 1 INCREMENT BY 50;


CREATE TABLE projects
(
    id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_projects PRIMARY KEY (id)
);

CREATE TABLE issues
(
    id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_issues PRIMARY KEY (id),
    CONSTRAINT fk_issues_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_issues_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);


CREATE INDEX idx_issues_project_id ON issues (project_id);
