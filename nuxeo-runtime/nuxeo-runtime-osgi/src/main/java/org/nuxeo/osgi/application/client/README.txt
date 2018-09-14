These demonstrates how to launch a client Nuxeo runtime and how to connect to a remote Nuxeo server 
To run a Nuxeo application you need forst to setupa  home directory for that application.
The home directory will be used as an working directory (various Nuxeo services will create here data or temporary files).

To run the samples in that package you need first to create a directory  that will be used as the home directory, then put somewhere in that directory the following JARs:
commons-logging-1.1.jar
jbossall-client-4.2.3.GA.jar
log4j-1.2.13.jar
nuxeo-common-1.5-SNAPSHOT.jar
nuxeo-core-api-1.5-SNAPSHOT.jar
nuxeo-core-client-1.5-SNAPSHOT.jar
nuxeo-core-query-1.5-SNAPSHOT.jar
nuxeo-core-schema-1.5-SNAPSHOT.jar
nuxeo-runtime-1.5-SNAPSHOT.jar
nuxeo-runtime-osgi-1.5-SNAPSHOT.jar
osgi-core-4.1.jar
xsom-20060306.jar

You can find these JArs in maven or more easily you can copy them from a Nuxeo WebEngine installation (or Nuxeo-EP installation)
Note that you will find xsom-20060306.jar inside nuxeo-core-schema-1.5-SNAPSHOT.jar 

These are the minimal JArs to be able to launch a Nuxeo client that can connect to a remote server.
If you don;t want to connect to a remote server but just to launch a Nuxeo application you can deploy only:
nuxeo-runtime-1.5-SNAPSHOT.jar
nuxeo-runtime-osgi-1.5-SNAPSHOT.jar
nuxeo-common-1.5-SNAPSHOT.jar
osgi-core-4.1.jar
commons-logging-1.1.jar
log4j-1.2.13.jar


The nuxeo-core-* jars are exposing the repository API so you need them if you want to connect to a repository.
To get more features you can deploy more JARs. For example you cna deploy all nuxeo-core JArs to get an embedded repository.
Note that some feature smay need additional configuration files, these files should be put in the config directory (create one if not exists)
Take a Nuxeo WebEngine distribution as an example on configuration files. 
For example of you want logging then put a log4j2.xml or log4j2.properties file in config directory.
  
Let assume you create a directory named "nuxeo: as a home directory and you pout the needed  JARs into nuxeo/lib directory.
Now you can start a nuxeo client by running the command:

java -Dnuxeo.bundles=lib/nuxeo-runtime-*:lib/nuxeo-*  -cp all:jars:here org.nuxeo.ecm.core.client.sample.Main org.nuxeo.ecm.core.client.HelloWorld  

The Main class will launch the HelloWolrd sample.
Also you must put all of the JArs you deployed in your classpath.
If you want to use a log4j.properties in config directory you must also put this directory in the classpath.  
The property
-Dnuxeo.bundles=lib/nuxeo-runtime-*:lib/nuxeo-*
will tell to the launcher to start any OSGI bundle that match the regular expressions (separaed by :)
You may notice that lib/nuxeo-runtime-* will match twice but this was specified like this to force nuexo-runtime deploying before other bundles.
(usually you may deploy in any order you want but some nuxeo services may require that runtime is already started).
Anyway, you can control the deployment order if you need this. 

Here is the content of a bash script that will start te client:

----
#!/bin/bash


java -Dnuxeo.bundles=lib/nuxeo-runtime-*:lib/nuxeo-* -cp config:lib/nuxeo-core-client-1.5-SNAPSHOT.jar:lib/nuxeo-runtime-osgi-1.5-SNAPSHOT.jar:lib/nuxeo-runtime-1.5-SNAPSHOT.jar:lib/nuxeo-common-1.5-SNAPSHOT.jar:lib/nuxeo-core-api-1.5-SNAPSHOT.jar:lib/nuxeo-core-query-1.5-SNAPSHOT.jar:lib/nuxeo-core-schema-1.5-SNAPSHOT.jar:lib/osgi-core-4.1.jar:lib/log4j-1.2.13.jar:lib/commons-logging-1.1.jar:lib/jbossall-client-4.2.3.GA.jar:lib/xsom-20060306.jar org.nuxeo.ecm.core.client.sample.Main $@ 
----

In order to run one of the sample in this package you should run:

./run.sh org.nuxeo.ecm.core.client.HelloWorld

or 

./run.sh org.nuxeo.ecm.core.client.HelloServer

The first client will simply write HelloWorld! on stdout after all Nuxeo bundles were installed.
The second one is connecting to a nuxeo-ep running on localhost and will print Hello Server! followed by the Root document in the repository 
 
 
 
