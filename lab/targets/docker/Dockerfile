# Docker unauthorized API access - Docker-in-Docker setup
# Exposes Docker daemon on TCP 2375 without authentication
FROM docker:20.10-dind

# Expose Docker API on TCP (no TLS, no auth)
EXPOSE 2375

# Start Docker daemon listening on TCP 2375
CMD ["--host=tcp://0.0.0.0:2375"]
ENTRYPOINT ["dockerd"]
