FROM postgres:9.6
MAINTAINER Corey Beres <corey.beres@gmail.com>
RUN mkdir -p /docker-entrypoint-initdb.d/
COPY db.sql /docker-entrypoint-initdb.d/
COPY pg_hba.conf /var/lib/pgsql96/data/

