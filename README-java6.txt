Warning about Java 6
====================

Currently the build process doesn't seem to lead to a usable application if
built using the JDK6.

You may want to build with the JDK5, however, and run with the JRE/JDK6.

In this case, you must take the following information from JBoss into account
(putting the 5 libraries mentioned below into the jboss/lib/endorsed/
directory seems to do the trick):

"""
* JBossAS 4.2.3.GA can be compiled with both Java5 & Java6. The Java5 compiled
  binary is our primary/recommended binary distribution. It has undergone
  rigorous testing and can run under both a Java 5 and a Java 6 runtime. 

  When running under Java 6 you need to manually copy the following libraries
  from the JBOSS_HOME/client directory to the JBOSS_HOME/lib/endorsed
  directory, so that the JAX-WS 2.0 apis supported by JBossWS are used:

  o jboss-jaxrpc.jar
  o jboss-jaxws.jar
  o jboss-jaxws-ext.jar
  o jboss-saaj.jar
  o jaxb-api.jar

* If you still have problems using JBoss with a Sun Java 6 runtime, you may
  want to set  -Dsun.lang.ClassLoader.allowArraySyntax=true, as described in
  JBAS-4491. Other potential problems under a Java 6 runtime include:

 o ORB getting prematurely destroyed when using Sun JDK 6 (see Sun Bug ID:
   6520484)
 o Unimplemented methods in Hibernate for JDK6 interfaces.

* When JBossAS 4.2.3 is compiled with Java 6, support for the extended JDBC 4
  API is included in the binary, however this can only be used under a Java 6
  runtime. In this case no manual configuration steps are necessary. It should
  be noted however that the Java 6 compiled distribution of JBoss AS 4.2.3.GA
  is still in experimental stage.
"""
