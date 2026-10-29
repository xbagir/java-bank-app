CREATE USER accounts_user WITH PASSWORD 'accounts_pass';
CREATE USER cash_user WITH PASSWORD 'cash_pass';
CREATE USER transfer_user WITH PASSWORD 'transfer_pass';
CREATE USER notifications_user WITH PASSWORD 'notifications_pass';

CREATE DATABASE accounts_db OWNER accounts_user;
CREATE DATABASE cash_db OWNER cash_user;
CREATE DATABASE transfer_db OWNER transfer_user;
CREATE DATABASE notifications_db OWNER notifications_user;

\connect accounts_db
ALTER SCHEMA public OWNER TO accounts_user;
GRANT ALL ON SCHEMA public TO accounts_user;

\connect cash_db
ALTER SCHEMA public OWNER TO cash_user;
GRANT ALL ON SCHEMA public TO cash_user;

\connect transfer_db
ALTER SCHEMA public OWNER TO transfer_user;
GRANT ALL ON SCHEMA public TO transfer_user;

\connect notifications_db
ALTER SCHEMA public OWNER TO notifications_user;
GRANT ALL ON SCHEMA public TO notifications_user;
