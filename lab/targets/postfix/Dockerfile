FROM ubuntu:22.04

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends postfix && \
    apt-get clean

EXPOSE 25

CMD ["postfix", "start-fg"]
