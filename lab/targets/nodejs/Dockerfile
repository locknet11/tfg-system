FROM node:18.16.0

WORKDIR /app

RUN echo '{"name":"vuln-app","version":"1.0.0","main":"server.js"}' > package.json

RUN echo 'const http = require("http");' > server.js && \
    echo 'const server = http.createServer((req, res) => {' >> server.js && \
    echo '  res.writeHead(200, {"Content-Type": "text/plain"});' >> server.js && \
    echo '  res.end("Vulnerable Node.js server running\\n");' >> server.js && \
    echo '});' >> server.js && \
    echo 'server.listen(3000, () => console.log("Server running on port 3000"));' >> server.js

EXPOSE 3000

CMD ["node", "server.js"]
