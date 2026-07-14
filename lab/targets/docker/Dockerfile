# Docker unauthorized API access - Docker-in-Docker setup (glibc target).
# Exposes the Docker daemon on TCP 2375 (and 8888) without authentication.
#
# Base is Debian (glibc) rather than the stock alpine dind image so that when the
# exploit — a privileged container with host root bind-mounted, chroot into it —
# lands in this container filesystem, it is a glibc userland with curl + base64.
# That lets a delivered glibc-linked native agent execute and the keyless HTTP
# installer run, completing the auto-replication chain. The Docker engine binaries
# are copied from the official dind image (statically linked; base libc is
# irrelevant for them).
#
# 8888 is also bound because nmap default top-1000 does not include 2375, so a
# plain `nmap -sV` (what the agent runs) would not discover it there; nmap
# fingerprints the service by protocol response, so it is still reported "docker".
# Engine pinned to 20.10 so the daemon reports "Docker remote API 20.10.24
# (API 1.41)", matching the lab's cached exploit mapping for this service.
FROM docker:20.10-dind AS engine

FROM debian:12-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        iptables curl ca-certificates openssl uidmap xz-utils e2fsprogs b3sum \
    && rm -rf /var/lib/apt/lists/*
COPY --from=engine /usr/local/bin/ /usr/local/bin/
EXPOSE 2375 8888
ENTRYPOINT ["dockerd-entrypoint.sh"]
CMD ["--host=tcp://0.0.0.0:2375", "--host=tcp://0.0.0.0:8888"]
