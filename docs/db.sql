-- https://wiki.postgresql.org/wiki/YUM_Installation
-- psql: postgres
-- kilgore rumfoord
DROP TABLE item;
DROP TABLE user_email_verify;
DROP TABLE user_password_reset;
DROP TABLE user_portrait;
DROP TABLE user_privacy;
DROP TABLE user_token;
DROP TABLE web_user;

ALTER TABLE web_user ALTER COLUMN password TYPE varchar(200);
ALTER TABLE user_token ALTER COLUMN token TYPE varchar(200);
ALTER TABLE user_password_reset ALTER COLUMN token TYPE varchar(200);
ALTER TABLE user_email_verify ALTER COLUMN token TYPE varchar(200);

CREATE TABLE IF NOT EXISTS web_user (
_id BIGSERIAL PRIMARY KEY
,username varchar(20) NOT NULL UNIQUE
,password varchar(200) NOT NULL
,email varchar(100) NOT NULL UNIQUE
,tz varchar(50) NOT NULL
,created timestamp DEFAULT current_timestamp
,activated timestamp
,deactivated timestamp
);
INSERT INTO web_user (username, password, email) VALUES
    ('admin', 'x', 'admin@7itemsorless.com', 'America/New_York'),
    ('help', 'x', 'help@7itemsorless.com', 'America/New_York'),
    ('info', 'x', 'info@7itemsorless.com', 'America/New_York'),
    ('7itemsorless', 'x', '7itemsorless@7itemsorless.com', 'America/New_York'),
    ('sevenitemsorless', 'x', 'sevenitemsorless@7itemsorless.com', 'America/New_York');

CREATE TABLE IF NOT EXISTS image (
_id BIGSERIAL PRIMARY KEY
,user_id bigint NOT NULL references web_user(_id)
,name varchar(256)
,path varchar(256) NOT NULL
,ext varchar(8) NOT NULL
,created timestamp DEFAULT current_timestamp
,public boolean DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_token (
 user_id bigint PRIMARY KEY references web_user(_id)
,token varchar(200) NOT NULL
,created timestamp DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS user_password_reset (
 user_id bigint PRIMARY KEY references web_user(_id)
,token varchar(200) NOT NULL
,created timestamp DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS user_email_verify (
 user_id bigint PRIMARY KEY references web_user(_id)
,token varchar(200) NOT NULL
,created timestamp DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS user_portrait (
 user_id bigint PRIMARY KEY references web_user(_id)
,image_id bigint NOT NULL references image(_id)
);

CREATE TABLE IF NOT EXISTS user_bio (
 user_id bigint PRIMARY KEY references web_user(_id)
,bio varchar(512) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_privacy (
 user_id bigint PRIMARY KEY references web_user(_id)
,profile boolean DEFAULT TRUE
,items boolean DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS item (
_id BIGSERIAL PRIMARY KEY
,user_id bigint NOT NULL references web_user(_id)
,image_id bigint references image(_id)
,title varchar(256)
,body varchar(4096)
,link varchar(2048)
,created timestamp DEFAULT current_timestamp
,public boolean DEFAULT TRUE
,comments boolean DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS item_comment (
_id BIGSERIAL PRIMARY KEY
,item_id bigint NOT NULL references item(_id)
,user_id bigint NOT NULL references web_user(_id)
,body varchar(4096) NOT NULL
,created timestamp DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS item_bias (
 item_id bigint NOT NULL references item(_id)
,user_id bigint NOT NULL references web_user(_id)
,bias char(1) NOT NULL
,created timestamp DEFAULT current_timestamp
,PRIMARY KEY (item_id, user_id)
);

CREATE TABLE IF NOT EXISTS follow (
 user_id bigint NOT NULL references web_user(_id)
,followed_id bigint NOT NULL references web_user(_id)
,created timestamp DEFAULT current_timestamp
,PRIMARY KEY (user_id, followed_id)
);

CREATE TABLE IF NOT EXISTS pending_follow (
 user_id bigint NOT NULL references web_user(_id)
,followed_id bigint NOT NULL references web_user(_id)
,approved boolean
,created timestamp DEFAULT current_timestamp
,PRIMARY KEY (user_id, followed_id)
);

