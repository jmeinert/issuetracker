ALTER TABLE issues
    ADD COLUMN reporter_id BIGINT,
    ADD COLUMN assignee_id BIGINT,
    ADD CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    ADD CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id) REFERENCES users (id);

CREATE INDEX idx_issues_reporter_id ON issues (reporter_id);
CREATE INDEX idx_issues_assignee_id ON issues (assignee_id);

-- Backfill existing issues with legacy reporter

INSERT INTO users (id, username, email, password_hash, role, enabled, created_at)
SELECT
    nextval('user_id_seq'),
    '__system_legacy_issue_reporter__',
    'legacy-issue-reporter@issuetracker.invalid',
    '{argon2id}$argon2id$v=19$m=19456,t=2,p=1$VO8vRyj497H7BshwaRKFiA$3V7vobsQABmWgOI9u4WppRSXctK8C9CepbBGelpsUN4',
    'USER',
    false,
    now()
WHERE EXISTS (SELECT 1 FROM issues);

UPDATE issues
SET reporter_id = (SELECT id FROM users WHERE username = '__system_legacy_issue_reporter__')
WHERE reporter_id IS NULL;

ALTER TABLE issues
    ALTER COLUMN reporter_id SET NOT NULL;
