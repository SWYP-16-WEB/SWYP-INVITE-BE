CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kakao_id VARCHAR(64) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_kakao_id UNIQUE (kakao_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    host_user_id BIGINT NULL,
    host_code_hash VARCHAR(64) NULL,
    share_code VARCHAR(64) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_type_custom VARCHAR(30) NULL,
    title VARCHAR(50) NOT NULL,
    event_at DATETIME NOT NULL,
    place VARCHAR(50) NOT NULL,
    rsvp_deadline DATE NULL,
    photo_url VARCHAR(500) NULL,
    reminder_at DATETIME NULL,
    reminder_message VARCHAR(500) NULL,
    settlement_info VARCHAR(512) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_events_host_code_hash UNIQUE (host_code_hash),
    CONSTRAINT uk_events_share_code UNIQUE (share_code),
    CONSTRAINT fk_events_host_user_id FOREIGN KEY (host_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_events_one_host CHECK ((host_user_id IS NULL) <> (host_code_hash IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE checklist_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    content VARCHAR(200) NOT NULL,
    needs_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_items_event_id FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rsvps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    name VARCHAR(30) NOT NULL,
    attendance VARCHAR(20) NOT NULL,
    email VARCHAR(512) NULL,
    reminder_status VARCHAR(20) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rsvps_event_id FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
