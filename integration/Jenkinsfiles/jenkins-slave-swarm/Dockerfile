FROM dockerpriv.nuxeo.com:443/nuxeo/jenkins-slave
RUN curl -o /opt/jenkins/bin/swarm-client.jar https://maven-eu.nuxeo.org/nexus/service/local/repositories/vendor-releases/content/org/jenkins-ci/plugins/swarm-client/3.12/swarm-client-3.12-remoting-2-59-2.jar
ADD ${SWARM}/myinit-setup-workspace.sh /etc/my_init.d/99-setup-worskpace.sh	
RUN chmod +x /etc/my_init.d/99-setup-worskpace.sh	
