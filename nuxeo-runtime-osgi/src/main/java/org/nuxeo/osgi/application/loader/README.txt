This package contains runtime loader that is used from Tomcat adapter to load nuxeo as a WAR
The Loader.loadFramework() is invoked by Tomcat loader via reflection and loads 
the OSGi adapter StandaloneApplication2.
StandaloneApplication2 is a rewrite of StandaloneApplication to better separate
the underlying loader (in our case tomcat) and the nuxeo osgi adapter class.
This class should replace in future the original StandaloneApplication class.
  