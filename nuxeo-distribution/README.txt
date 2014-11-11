-----
About
-----

See http://doc.nuxeo.org/ for full documentation.

This module builds the Nuxeo products: Nuxeo EP, Nuxeo DM, Nuxeo Shell, ...


1. Required tools

Building Nuxeo products requires the following tools:
  * Java Sun 5 or 6
  * Apache Maven 2.2.1
  * Apache Ant 1.7.1+
  * IS Tools for building Windows installers
  * Wine for calling IS Tools from Mac/Linux OS
  * Open Source tools that will be downloaded by Maven when needed.


2. Build ways

There are various build ways.

Java and Maven are at the lowest level, all configuration about building a module
is given in the Maven POM file. So, everything can be built using Maven but it 
requires some knowledge about Nuxeo and its packagings.

Ant is available at the top level: Ant targets have been defined to provide 
user-friendly commands for building most used products.


3. Modules listing

  * nuxeo-platform-ear : Nuxeo Enterprise Platform EAR
  * nuxeo-distribution-dm : Nuxeo Document Management EAR
  * nuxeo-distribution-shell : Nuxeo Shell
  * nuxeo-distribution-jboss : JBoss containing Nuxeo EP or DM
  * nuxeo-distribution-gf3 : GlassFish distribution
  * nuxeo-distribution-jetty : Jetty containing Nuxeo EP or DM
  * nuxeo-distribution-tomcat : Tomcat distribution
  * nuxeo-distribution-windows : Windows installers
  * nuxeo-distribution-base : template package for use by other modules.
  * nuxeo-distribution-server : template package for use by other modules.
  * nuxeo-distribution-jetty-base : template package for use by other modules.
  * nuxeo-distribution-izpack : DEPRECATED, was used to build IzPack installers.

--------------------------------
Building predefined applications
--------------------------------

1. Predefined applications

1.1. Nuxeo EP

  Built EAR is in nuxeo-platform-ear/target/ and its name depends on chosen
  package: default is nuxeo.ear


1.2. Nuxeo Document Management

  Built EAR is in nuxeo-distribution-dm/target/


1.3. Nuxeo Shell 

  A command-line client application suitable to connect to remote nuxeo servers.
  This application can be used to connect to remote nuxeo servers for debugging,
  browsing or administration purposes.

  Built application is in nuxeo-distribution-shell/target/
    
    
1.4. Nuxeo Core Server 

  A minimal server application. An embedded repository will be started. No other
  platform services are available.
  
  This application can be used to debug, test or develop nuxeo components that
  need a repository connection.
  
  Remoting will be also available in the future via Nuxeo Runtime.
  
  Built application is in nuxeo-distribution-server/target/


1.5. Nuxeo Jetty Server Base

  A Nuxeo server application embedding a Jetty server. 
  This application can be used to browse repository content via WEB.
  This is also known as Nuxeo WebEngine (based on Jetty).

  Built application is in nuxeo-distribution-jetty-base/target/


1.6. Nuxeo GF3 Server

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


1.7. Nuxeo Tomcat WebApp

  A Nuxeo Server packaged as an exploded WAR for Tomcat v6.
  This build will generate a zip containing a 'tomcat' directory. You need to
  copy the content of this directory to your installed Tomcat. Then restart
  Tomcat.

  Nuxeo WebEngine will be available at htpp://localhost:8080/nuxeo

  Built application is in nuxeo-distribution-tomcat/target/


2. Build with Maven

Building from nuxeo root won't run nuxeo-distribution.
Building from nuxeo-distribution with no specific profile will package Nuxeo EP, 
Nuxeo DM on VCS, JBoss with Nuxeo DM on VCS.
Meaning "mvn clean package" produces:
  * Nuxeo EP EAR
    nuxeo-platform-ear/target/nuxeo.ear.zip
  * Nuxeo EP resource files (for various backends or deployments) for EAR
    nuxeo-platform-ear/target/nuxeo-platform-ear-5.3.1-SNAPSHOT-*.zip
  * Nuxeo DM EAR with default VCS backend 
    nuxeo-distribution-dm/target/nuxeo-distribution-dm-5.3.1-SNAPSHOT.zip
  * JBoss 4.2.3.GA containing Nuxeo DM with default VCS backend
    nuxeo-distribution-jboss/target/nuxeo-distribution-jboss-5.3.1-SNAPSHOT-nuxeo-dm.zip

Building other alternatives for a product is done with Maven profiles:
  * mvn clean package -P<PRODUCT>,<BACKEND>


3. Understanding Maven profiles and classifiers

Profiles are mainly used to manage the list of classifiers being generated.
Maven plugins rely by default on a such mechanism for creating tests, sources and 
javadoc jars. It is usable also for any other specific builds (OS, JDK, env, packaging, ...). 
It's widely used by a lot of third-parties (google gwt, json, shindings, ...). 
Think about "classifiers" as "qualifiers" (sources, javadoc, tests, linux, windows, 
mac, jta, all, ...). For example, the following are two alternatives ("classifiers") 
for the package ("artifact") named "nuxeo-distribution-tomcat":
  * nuxeo-distribution-tomcat-5.3.1-SNAPSHOT-nuxeo-dm-jtajca.zip
  * nuxeo-distribution-tomcat-5.3.1-SNAPSHOT-nuxeo-dm.zip

Some profiles are used to choose the product to build. Other profiles are used to
choose which alternatives (classifiers) of the product will be built. Multiple 
profiles can be used simultaneously. 
Here are some common profiles and their impact on build result:
  * all-distributions: build everything except the Windows installer
  * all: build all classifiers for the called module(s) 
  * nuxeo-ep: build only Nuxeo EP classifier (if JBoss module, so build only
    JBoss with Nuxeo EP)
  * nuxeo-dm: same as nuxeo-ep but with Nuxeo DM
  * shell: package a Nuxeo Shell
  * jboss: package a Nuxeo within JBoss

Here are some usage examples (ran from nuxeo-distribution):
  * (default) Building Nuxeo EP, nuxeo DM with VCS, JBoss with Nuxeo DM on VCS
    o mvn clean package
    o mvn clean package -Pjboss,nuxeo-dm,vcs
  * Building all Nuxeo DM alternatives
    o mvn clean package -Pnuxeo-dm,all
  * Building all JBoss packagings
    o mvn clean package -Pjboss,all
  * Building a JBoss packaging and a Nuxeo EP EAR based on MySQL
    o mvn clean package -Pjboss,nuxeo-ep,mysql
  * Building all Nuxeo products and their alternatives
    o mvn clean package -Pall-distributions

Note: because of a Maven bug making things crazy when two classifiers of an
artifact are not deployed at the same time (i.e. if you deploy only nuxeo-ep 
classifier of nuxeo-distribution-jboss, then nuxeo-dm classifier becomes 
unreachable from local and remote Maven repositories).
That means when you want to "deploy" (Maven remote deployment) or "install" (Maven
local deployment) a module, you must use "all" profile.

When you need only one classifier, for any other purpose than
install/deploy to m2 repository, then you can use the dedicated profiles.


4. Main products and available profiles:

Modules with an asterisk need refactoring and may be unusable for now: foo*
Infrastructure profiles are within parenthesis: (foo)
Default profiles are within asterisks: *foo*

  * Module nuxeo-platform-ear
    o Produced artifacts
      nuxeo-platform-ear-5.3.1-SNAPSHOT.pom
      nuxeo-platform-ear-5.3.1-SNAPSHOT.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-common.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-derby.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-h2.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-jcr-postgresql.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-jcr.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-mono.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-mysql.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-oracle.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-platform-stateful.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-postgresql.zip
      nuxeo-platform-ear-5.3.1-SNAPSHOT-resources-web-stateless.zip
    o Available profiles
      derby
      mysql
      postgresql
      oracle
      h2
      jcr-profile
      jcr-postgresql
      *vcs-profile*
      
  * Module nuxeo-distribution-dm
    o Produced artifacts
      nuxeo-distribution-dm-5.3.1-SNAPSHOT.pom
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-derby.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-h2.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-jcr-postgresql.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-jcr.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-mysql.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-oracle.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful-derby.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful-h2.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful-mysql.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful-oracle.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful-postgresql.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-platform-stateful.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-postgresql.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT-web-stateless.zip
      nuxeo-distribution-dm-5.3.1-SNAPSHOT.zip
    o Available profiles
      all-distributions
      all
      (nuxeo-ep)
      *vcs*
      derby
      mysql
      postgresql
      oracle
      h2
      jcr
      jcr-postgresql
      
  * Module nuxeo-distribution-jboss
    o Produced artifacts
      nuxeo-distribution-jboss-5.3.1-SNAPSHOT-nuxeo-dm.zip
      nuxeo-distribution-jboss-5.3.1-SNAPSHOT-nuxeo-ep.zip
    o Available profiles
      all-distributions
      all
      nuxeo-ep
      *nuxeo-dm*

  * Module nuxeo-distribution-shell
    o Produced artifacts
      nuxeo-distribution-shell-5.3.1-SNAPSHOT.zip
    o No available profile
    
  * Module nuxeo-distribution-jetty*
    o Produced artifacts
      nuxeo-distribution-jetty-5.3.1-SNAPSHOT-nuxeo-dm.zip
      nuxeo-distribution-jetty-5.3.1-SNAPSHOT-nuxeo-ep.zip
    o Available profiles
      *nuxeo-dm*
      nuxeo-ep
      
  * Module nuxeo-distribution-gf3*
    o Produced artifacts
      nxserver.zip
    o Available profiles
      ra
      jcr
      *vcs*

  * Module nuxeo-distribution-tomcat*
    o Produced artifacts
      nuxeo-distribution-tomcat-5.3.1-SNAPSHOT-nuxeo-dm-jtajca.zip
      nuxeo-distribution-tomcat-5.3.1-SNAPSHOT-nuxeo-dm.zip
    o No available profile

  * Module nuxeo-distribution-windows*
    o Produced artifacts
      nuxeo-dm-5.3.0-GA-nuxeo-ep-setup.exe
      nuxeo-dm-5.3.0-GA_64-nuxeo-ep-setup.exe
      nuxeo-dm-5.3.0-GA-nuxeo-dm-setup.exe
      nuxeo-dm-5.3.0-GA_64-nuxeo-dm-setup.exe
    o Available profiles
      *nuxeo-dm*
      nuxeo-ep


5. Easy build with Ant

With Ant and user input:
  * from nuxeo-distribution root, run "ant distrib" and choose the distribution
  you want to build.

With Ant, no user input:
  * run "ant distrib -Ddistrib=<PRODUCT>"
  * run "ant distrib -Ddistrib=<PRODUCT>,<BACKEND>"

For example:
  * "ant distrib -Ddistrib=nuxeo-dm,postgresql"
  * "ant distrib -Ddistrib=nuxeo-ep,mysql"
  
Default repository is VCS but it is possible to set JCR+Derby or JCR+PostgreSQL, 
see nuxeo-distribution-dm-5.3.1-SNAPSHOT-jcr-postgresql.zip
Note that this configuration requires some manual changes into
$JBOSS/server/default/conf/login-config.xml


------------
Custom build
------------

It is of course possible to create custom builds.

For historical reasons, there are multiple technologies used for packaging in this 
project (maven-assembly-plugin, maven-nuxeo-plugin, maven-antrun-extended-plugin,
nuxeo-distribution-tools). 

They are all based on Maven principles with the objectives to avoid duplication, 
ease maintenance and upgrade, rely on Maven artifacts, be OS independant.

We recommmend to use our newest tool "nuxeo-distribution-tools".

Execution of the assembly may be done from Maven execution as a Maven plugin, 
from command line or from Ant.

Based on Ant syntax, it provides access to major Maven concepts.

Principles of an assembly are generally to: 

* retrieve a Maven dependency tree
* use this dependency tree to dispatch artifacts into directories
* download complementary artifacts (default packaging, resources, ...)
* download empty server (JBoss, Jetty, Tomcat, ...)
* assemble all those parts into a runnable product.

Please see the chosen tool documentation for more details.
