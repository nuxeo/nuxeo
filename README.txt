About
-----

See http://doc.nuxeo.org/ for full documentation.

This module builds the Nuxeo products: Nuxeo EP, Nuxeo WebEngine, Nuxeo DM,
Nuxeo Shell...

There are various build ways. Easiest is from root with Ant but you can also
build from any sub-module with Maven. Ant build will call a Maven build with
default values.

Modules listing:

- nuxeo-platform-ear to build Nuxeo Enterprise Platform EAR
- nuxeo-distribution-dm to build a Nuxeo Document Management EAR
- nuxeo-distribution-jboss to package a JBoss containing Nuxeo EP or DM EAR
- nuxeo-distribution-shell to build Nuxeo Shell
- nuxeo-distribution-gf3 to build GlassFish distribution
- nuxeo-distribution-jetty to package Jetty containing Nuxeo EP or DM
- nuxeo-distribution-jetty-ep to build Jetty distribution (DEPRECATED)
- nuxeo-distribution-tomcat to build Tomcat distribution

- nuxeo-distribution-base, nuxeo-distribution-server are used by other modules.
- nuxeo-distribution-jetty-base is for use by other modules.
- nuxeo-distribution-izpack is obsolete. Was used to build an IzPack installer from a Nuxeo EP zip archive.
- nuxeo-distribution-tools is a prototype. Future distribution tools.

Building predefined applications
--------------------------------

With Ant and user input:
  - from nuxeo-distribution root, run "ant distrib" and choose the distribution
    you want to build.

With Ant, no user input:
  - run "ant distrib -Ddistrib=PROFILE"
  - run "ant distrib -Ddistrib=PROFILE,BACKEND"

With Maven, no user input: 
  - run "mvn clean install package -PPROFILE -Dmaven.test.skip=true"

Available values for "PROFILE" are:
  nuxeo-ep, nuxeo-ep-jboss, nuxeo-dm, nuxeo-dm-jboss, shell, jetty, gf3,
  tomcat, core, all-distributions

It can be followed by ",BACKEND"; available values depend on PROFILE used.

Nuxeo EP and DM have those backend configurations available: derby, mysql,
postgresql, oracle and h2.

For example:
  - "ant distrib -Ddistrib=nuxeo-dm,postgresql"
  - "ant distrib -Ddistrib=nuxeo-ep,mysql"
  
Default repository is VCS but it is also possible to set JCR+Derby or
JCR+PostgreSQL, for example:
  - "ant distrib -Ddistrib=nuxeo-dm,postgresql -Dmvn.opts=-Djcr"

Note that this configuration requires some manual changes into
$JBOSS/server/default/conf/login-config.xml

Glassfish has those available configurations (vcs, vcs+ra, jcr+ra):
  - "ant distrib -Ddistrib=gf3,vcs"
  - "ant distrib -Ddistrib=gf3,vcs,ra"
  - "ant distrib -Ddistrib=gf3,jcr,ra"

1. Nuxeo EP

  Built EAR is in nuxeo-platform-ear/target/ and its name depends on chosen
  package: default is nuxeo.ear

2. Nuxeo Document Management

  Built EAR is in nuxeo-distribution-dm/target/

3. Nuxeo Shell 

  A command-line client application suitable to connect to remote nuxeo servers.
  This application can be used to connect to remote nuxeo servers for debugging,
  browsing or administration purposes.

  Built application is in nuxeo-distribution-shell/target/
    
4. Nuxeo Core Server 

  A minimal server application. An embedded repository will be started. No other
  platform services are available.
  
  This application can be used to debug, test or develop nuxeo components that
  need a repository connection.
  
  Remoting will be also available in the future via Nuxeo Runtime.
  
  Built application is in nuxeo-distribution-server/target/
    
5. Nuxeo Jetty Server Base

  A Nuxeo server application embedding a Jetty server. 
  This application can be used to browse repository content via WEB.
  This is also known as Nuxeo WebEngine (based on Jetty).

  Built application is in nuxeo-distribution-jetty-base/target/
   
6. Nuxeo GF3 Server

  A Nuxeo server application embedding a GlassFish v3 server.
  This application can be used to browse a repository content via the web.
  This is also known as Nuxeo WebEngine (based on GF3).
  In the future, this application will provide a full installation of Nuxeo
  (including EJBs, JMS, etc).

  Built application is in nuxeo-distribution-gf3/target/

  By default the build will generate a server based on JCR repository backend
  without RA enabled.

  You can customize your builds using profiles as following:
    - "mvn install -Pgf3,vcs" - a server using VCS repository backend
    - "mvn install -Pgf3,vcs,ra" - a server using VCS repository backend + RA
      support
    - "mvn install -Pgf3,jcr,ra" - a server using JCR based repository backend
      + RA support
    
6. Nuxeo Tomcat WebApp

  A Nuxeo Server packaged as an exploded WAR for Tomcat v6.
  This build will generate a zip containing a 'tomcat' directory. You need to
  copy the content of this directory to your installed Tomcat. Then restart
  Tomcat.

  Nuxeo WebEngine will be available at htpp://localhost:8080/nuxeo

  Built application is in nuxeo-distribution-tomcat/target/


Extending
---------

This project manages the build of standalone Nuxeo applications such as nxshell
and WebEngine.

The build logic is slightly different from the nuxeo.ear build and is more
adapted for light packagings composed of well defined nuxeo artifact subsets.

The main difference is the way the final ZIPs are assembled. Instead of using
assembly descriptor inheritance, the final ZIP is assembled from several
prebuilt ZIPs by using an assembly descriptor.

Let's see for example how webengine based on jetty is built: as webengine is
using nxshell infrastructure, it need to share with this project the basic
infrastructure. On this skeleton it should add server specific artifacts and
configuration. Then it should add jetty specific config and JARs.

All these intermediate builds should be reusable to build other applications
like webengine based on GlassFish v3 or even nxshell itself.

To do this, 5 projects were created:

- A: nuxeo-distribution-base - contains basic skeleton of a standalone
  application)

- B: nuxeo-distribution-shell - modifies the ZIP generated by A (by
  adding/overriding or removing entries)

- C: nuxeo-distribution-server - modifies the ZIP generated by A by adding
  server related artifacts and configuration

- D: nuxeo-distribution-jetty-base - modifies the ZIP generated by C by adding jetty
  specific JARs and configuration

- E: nuxeo-distribution-gf3 - modifies the ZIP generated by C by adding GF3
  specific JARs and configuration

This way, the build is reused more easily than by using assembly inheritance,
since any modification on an intermediate artifact (ZIP) is automatically
visible in the projects that are overriding the artifact.

NB: we are not using for now categories on Nuxeo-Shell and Nuxeo-WebEngine
since the projects are not so complex to manage by using explicit dependencies
in assembly files. Also, existing categories may not fit well with the kind of
build done for nxshell or webengine. 

Maybe in the future we will use categories too, if projects become too complex.

