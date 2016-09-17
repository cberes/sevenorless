FROM java:8
MAINTAINER Corey Beres <corey.beres@gmail.com>

RUN apt-get update && apt-get install -y \
    wget \
    git-core \
    postgresql93-server

COPY pg_hba.conf /var/lib/pgsql93/data/
RUN service postgresql93 initdb
RUN service postgresql93 start

WORKDIR /opt/sevenorless
COPY target/sevenorless-0.1.0-SNAPSHOT-standalone.jar sevenorless.jar

CMD ["java", "-jar", "sevenorless.jar"]
