# Tomcat CVE-2017-12615 - PUT method file upload to RCE
FROM tomcat:8.5.19

# Enable read/write on DefaultServlet so PUT requests can upload files
# This is the root cause of CVE-2017-12615
RUN sed -i 's|<Connector port="8080"|<Connector port="8080" allowTrace="true"|' /usr/local/tomcat/conf/server.xml && \
    sed -i '/<servlet-name>default<\/servlet-name>/,/<\/servlet>/ s|</servlet>|<init-param>\n            <param-name>readonly</param-name>\n            <param-value>false</param-value>\n        </init-param>\n    </servlet>|' /usr/local/tomcat/conf/web.xml

EXPOSE 8080
CMD ["catalina.sh", "run"]
