FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends bind9 bind9-host bind9utils && \
    apt-get clean

EXPOSE 53/udp 53/tcp

CMD ["named", "-g", "-c", "/etc/bind/named.conf", "-u", "bind"]
