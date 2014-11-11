How to start nuxeo inside tomcat

1. put nuxeo-runtime-tomcat-adapter.jar into tomcat lib dir.
2. put derby.jar into tomcat lib dir. (or any other database jars you use)
3. create a WAR and put a META-INF/context.xml by copying the content from the context.xml-template file.


------------------------

Why it is needed the context.xml file?
This file is used to overide the tomcat context to be able to install a classloader that will be used by nuxeo
and then to start the framework after the context is started.

The custom WebApp classloader will insert a URLClassLoader parent in the classloader chain
and pass it to nuxeo to use it to start the framework.
The nuxeo classloader will be unique inside tomcat - since you only can have a single nuxeo instance inside tomcat.
You can deploy multiple wars that are using nuxeo 9and start it if needed). These webapps will use the same nuxeo classloader as their parent classloader so that any webapp will see nuxeo classes.
Web classes must be put in WEB-INF/lib and will not be visible between different nuxeo wars.
This mechanism allows you having multiple wars sharing the sdame nuxeo instance.
The first started webapp will also start nuxeo - the next ones will only retrieve the nuxeo class loader and use it.
The last stoped webapp will stop nuxeo. 
For this to work you need to add the context.xml file described above.

context.xml configuration
-------------------------

In the context.xml file you should specify the location of nuxeo application and the location nuxeo-runtime-osgi JAR.
To specify a location you can use system properties like:



    
    
------------------------- 
For TX and JCA pooling see 
http://tomcat.apache.org/tomcat-4.1-doc/jndi-datasource-examples-howto.html#Tyrex%20Connection%20Pool

    * tyrex-1.0.jar
    * ots-jts_1.0.jar
    * jta_1.0.1.jar
    * xerces-J_1.4.0.jar
    * castor
    * log4j
   