-----
About
-----

See http://doc.nuxeo.org/ for full documentation.

This module builds the Nuxeo products: Nuxeo CAP, Nuxeo DM, Nuxeo Shell, ...

1. Quick build

Here are common commands for building distributions:
  * Building Nuxeo DM with JBoss
    o mvn clean install -Pjboss,nuxeo-dm
    
  * Building all JBoss packagings
    o mvn clean install -Pjboss,all
    
  * Building Nuxeo DM with Tomcat
    o mvn clean install -Ptomcat,nuxeo-dm
    
  * Building all Tomcat packagings
    o mvn clean install -Ptomcat,all
    
  * Building all Nuxeo products and their alternatives
    o mvn clean install -Pall-distributions

2. Required tools

Building Nuxeo products requires the following tools:
  * Java Sun 5 or 6
  * Apache Maven 2.2.1
  * Apache Ant 1.7.1+
  * IS Tools for building Windows installers
  * Wine for calling IS Tools from Mac/Linux OS
  * Open Source tools that will be downloaded by Maven when needed.


3. Build ways

There are various build ways.

Java and Maven are at the lowest level, all configuration about building a module
is given in the Maven POM file. So, everything can be built using Maven but it 
requires some knowledge about Nuxeo and its packagings.

Ant is available at the top level: Ant targets have been defined to provide 
user-friendly commands for building most used products.


4. Modules listing

  * nuxeo-distribution-cap: Nuxeo Content Application Platform EAR
  * nuxeo-distribution-dm: Nuxeo Document Management EAR
  * nuxeo-distribution-shell: Nuxeo Shell
  * nuxeo-distribution-jboss: JBoss containing Nuxeo CAP or DM
  * nuxeo-distribution-gf3: (DEPRECATED) GlassFish distribution
  * nuxeo-distribution-jetty: Jetty containing Nuxeo CAP or DM
  * nuxeo-distribution-tomcat: Tomcat distribution
  * nuxeo-distribution-base: (DEPRECATED) template package for use by other modules.
  * nuxeo-distribution-server: (DEPRECATED) template package for use by other modules.
  * nuxeo-distribution-jetty-base: template package for use by other modules.
  * nuxeo-distribution-izpack: (DEPRECATED) Used to build IzPack installers.
  * nuxeo-distribution-resources: Resources archives used in other packagings (doc, binaries, templates).
  * nuxeo-windows-startup: Nuxeo control panel for Windows (requires Mono or .Net).

5. Build with Maven

Building from nuxeo root won't run nuxeo-distribution.
Building from nuxeo-distribution with no specific profile will package Nuxeo CAP, 
Nuxeo DM on VCS, JBoss with Nuxeo DM on VCS.
Meaning "mvn clean install" produces:
  * Nuxeo CAP EAR
    nuxeo-distribution-cap/target/nuxeo.ear.zip
  * Nuxeo CAP resource files (for various backends or deployments) for EAR
    nuxeo-distribution-cap/target/nuxeo-distribution-cap-5.4.0-SNAPSHOT-*.zip
  * Nuxeo DM EAR with default VCS backend 
    nuxeo-distribution-dm/target/nuxeo-distribution-dm-5.4.0-SNAPSHOT.zip
  * JBoss 4.2.3.GA containing Nuxeo DM with default VCS backend
    nuxeo-distribution-jboss/target/nuxeo-distribution-jboss-5.4.0-SNAPSHOT-nuxeo-dm.zip

Building other alternatives for a product is done with Maven profiles:
  * mvn clean install -P<PRODUCT>,<BACKEND>


6. Understanding Maven profiles and classifiers

Profiles are mainly used to manage the list of classifiers being generated.
Maven plugins rely by default on a such mechanism for creating tests, sources and 
javadoc jars. It is usable also for any other specific builds (OS, JDK, env, packaging, ...). 
It's widely used by a lot of third-parties (google gwt, json, shindings, ...). 
Think about "classifiers" as "qualifiers" (sources, javadoc, tests, linux, windows, 
mac, jta, all, ...). For example, the following are two alternatives ("classifiers") 
for the package ("artifact") named "nuxeo-distribution-tomcat":
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm-jtajca.zip
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm.zip

Some profiles are used to choose the product to build. Other profiles are used to
choose which alternatives (classifiers) of the product will be built. Multiple 
profiles can be used simultaneously. 
Here are some common profiles and their impact on build result:
  * all-distributions: build everything except the Windows installer
  * all: build all classifiers for the called module(s) 
  * nuxeo-cap: build only Nuxeo CAP classifier (if JBoss module, so build only
    JBoss with Nuxeo CAP)
  * nuxeo-dm: same as nuxeo-cap but with Nuxeo DM
  * shell: package a Nuxeo Shell
  * jboss: package a Nuxeo within JBoss

Here are some usage examples (ran from nuxeo-distribution):
  * (default) Building Nuxeo CAP, nuxeo DM with VCS, JBoss with Nuxeo DM on VCS
    o mvn clean install
    o mvn clean install -Pjboss,nuxeo-dm,vcs
  * Building all Nuxeo DM alternatives
    o mvn clean install -Pnuxeo-dm,all
  * Building all JBoss packagings
    o mvn clean install -Pjboss,all
  * Building all Nuxeo products and their alternatives
    o mvn clean install -Pall-distributions

Note: because of a Maven bug making things crazy when two classifiers of an
artifact are not deployed at the same time (i.e. if you deploy only nuxeo-cap 
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

  * Module nuxeo-distribution-cap
    o Produced artifacts
      nuxeo-distribution-cap-5.4.0-SNAPSHOT.pom
      nuxeo-distribution-cap-5.4.0-SNAPSHOT.zip
    o No available profile
      
  * Module nuxeo-distribution-dm
    o Produced artifacts
      nuxeo-distribution-dm-5.4.0-SNAPSHOT.pom
      nuxeo-distribution-dm-5.4.0-SNAPSHOT.zip
    o No available profile
      
  * Module nuxeo-distribution-jboss
    o Produced artifacts
      nuxeo-distribution-jboss-5.4.0-SNAPSHOT-nuxeo-cap.zip
      nuxeo-distribution-jboss-5.4.0-SNAPSHOT-nuxeo-dm.zip
    o Available profiles
      all-distributions
      *all*
      nuxeo-cap
      nuxeo-dm

  * Module nuxeo-distribution-shell
    o Produced artifacts
      nuxeo-distribution-shell-5.4.0-SNAPSHOT.zip
    o No available profile
    
  * Module nuxeo-distribution-jetty*
    o Produced artifacts
      nuxeo-distribution-jetty-5.4.0-SNAPSHOT-nuxeo-dm.zip
      nuxeo-distribution-jetty-5.4.0-SNAPSHOT-nuxeo-cap.zip
    o Available profiles
      all-distributions
      *all*
      nuxeo-dm
      nuxeo-cap
      
  * Module nuxeo-distribution-gf3* (DEPRECATED)
    o Produced artifacts
      No more build result
    o Available profiles
      ra
      *vcs*

  * Module nuxeo-distribution-tomcat
    o Produced artifacts
      nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm.zip
      nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-cap.zip
      nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-coreserver.zip
    o Available profiles
      all-distributions
      *all*
      nuxeo-cap
      nuxeo-dm

  * Module nuxeo-windows-startup
    o Produced artifacts
      NuxeoCtl.exe
    o No available profile

  * Module nuxeo-distribution-resources
    o Produced artifacts
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-bin.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-doc.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-jetty-base.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-templates-common.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-templates-common-dm.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-templates-jboss.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-templates-tomcat.zip
      nuxeo-distribution-resources-5.4.0-SNAPSHOT-templates-tomcat-dm.zip
    o No available profile


5. Easy build with Ant

IMPORTANT: backend specific builds are deprecated. Use of <BACKEND> profiles is also deprecated.
Backend is chosen just before starting the server, while configuring bin/nuxeo.conf. 

With Ant and user input:
  * from nuxeo-distribution root, run "ant distrib" and choose the distribution you want to build.

With Ant, no user input:
  * run "ant distrib -Ddistrib=<PRODUCT>"

For example:
  * "ant distrib -Ddistrib=nuxeo-dm"
  * "ant distrib -Ddistrib=tomcat"
  
-------------------------------------
Details about predefined applications
-------------------------------------

1. Nuxeo CAP

  Built EAR is in nuxeo-distribution-cap/target/ and its name depends on chosen
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

  You can customize your builds using profiles as following:

    - "mvn install -Pgf3,vcs" - a server using VCS repository backend
    - "mvn install -Pgf3,vcs,ra" - a server using VCS repository backend + RA
      support


7. Nuxeo Tomcat WebApp

  A Nuxeo Server packaged as an exploded WAR for Tomcat v6.
  This build will generate a zip containing a 'tomcat' directory. You need to
  copy the content of this directory to your installed Tomcat. Then restart
  Tomcat.

  Nuxeo WebEngine will be available at htpp://localhost:8080/nuxeo

  Built application is in nuxeo-distribution-tomcat/target/

------------
Custom build
------------

It is of course possible to create custom builds.

For historical reasons, there are multiple technologies used for packaging in
nuxeo-distribution project (maven-assembly-plugin, maven-nuxeo-plugin,
maven-antrun-extended-plugin, nuxeo-distribution-tools).

They are all based on Maven principles with the objectives to avoid duplication, 
ease maintenance and upgrade, rely on Maven artifacts, be OS independant.

We recommend to use our newest tool "nuxeo-distribution-tools".

Execution of the assembly may be done from Maven execution as a Maven plugin, 
from command line or from Ant.

Based on Ant syntax, it provides access to major Maven concepts and Ant flexibility.

Principles of an assembly are generally to:
    * inherit a Maven dependency tree (list of artifacts to retrieve)
    * use this dependency tree to dispatch artifacts into directories
    * download complementary artifacts (default packaging, resources, drivers, ...)
    * download an empty server (JBoss, Jetty, Tomcat, ...)
    * assemble all those parts into a runnable product.

See the chosen tool documentation for more details.
