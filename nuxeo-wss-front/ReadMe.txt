
About WSS Handler
=================

This modules contains the needed code to handle WSS calls.
Everything is currently packaged as a Filter.

WSS protocols
-------------
WSS protocols covers several stuffs :
 - WebDav protocol : legacy browse
 - FrontPage extensions  : browse
 - CAML : dialog with Sharepoint and MSOffice
 - WebServices : dialog with Sharepoint and MSOffice

 WSS-Handler does not try to implement all.
 The original implementation was started based on the public specifications,
 but quickly moved to using ngrep to get the network traffic.
 There are 2 reasons for that :
  - what is used by Explorer and MSOffice is a very small subset of WSS APis and protocols
    and it is very hard from the documentation to know what is really used
  - documentation does not always match what is really sent over the wire ...

Target scope
------------
The target scope of this implementation is :
 - Browse / Open / Save documents from MSOffice 2K3 / 2K7
 - Browse / Open / Save documents from Explorer if MSOffice 2K3 / 2K7 is installed
 - Display some meta-data information in MSOffice 2K3 / 2K7

About WebServices
-----------------

According to the target scope, WSS-Handler only needs to implement 2 methods of 2 webservices :
 - one returning a string
 - one returning an XML document

For simplicity, these 2 WebServices where implemented in pure freemarker.
In future versions, this may be migrated to true WebService (JAX-WS).

Handler vs Backend
==================

The generic handler is composed of 2 parts :
 - the protocol handler (Filter, parsers, ....)
  => handles WSS requests / responses
 - the backend that does provide access to the documents and meta-data

The generic handler comes with :
 - a SPI that can be used to provide external backend
 - an "In Memory" backend

Test installation
=================

Because Explorer / MSOffice always issue some http request at the root of the server,
the WSS-handler has to be configured on /.

For a simple test setup, you must declare it in the Root context path of your servlet container.

    <filter>
      <display-name>WSS Filter</display-name>
      <filter-name>WSSFilter</filter-name>
      <filter-class>org.nuxeo.wss.servlet.WSSFilter</filter-class>
    </filter>
    <filter-mapping>
      <filter-name>WSSFilter</filter-name>
      <url-pattern>/*</url-pattern>
    </filter-mapping>

For JBoss 4.2.3, the target file is :
 $JBOSS/server/default/deploy/jboss-web.deployer/ROOT.war/WEB-INF/web.xml

The wss-handler jar should be in the classpath of the Root webapp
=> $JBOSS/server/default/lib for JBoss

2 additionnal jars are needed :
 - apache commons-lang-2.2
 - freemarker-2.3.11
=> $JBOSS/server/default/lib for JBoss

Nuxeo setup
===========

In order to 2 test WSS against Nuxeo, you have 2 configurations to be made :
 1 - setup the root filter and set it in proxy mode
 2 - deploy the nuxeo-wss-backend

Step 1
------
Step one can be done in the root web.xml by just adding the org.nuxeo.wss.rootFilter param:
   <filter>
      <display-name>WSS Filter</display-name>
      <filter-name>WSSFilter</filter-name>
      <filter-class>org.nuxeo.wss.servlet.WSSFilter</filter-class>
      <init-param>
        <param-name>org.nuxeo.wss.rootFilter</param-name>
        <param-value>/nuxeo</param-value>
      </init-param>
    </filter>
    <filter-mapping>
      <filter-name>WSSFilter</filter-name>
      <url-pattern>/*</url-pattern>
    </filter-mapping>

NB : the wss-handler jar needs to stay in $JBoss/server/default/lib
     (no need to deploy it inside Nuxeo)

Step 2
------
Deploy nuxeo-platform-wss-backend in nuxeo.ear/plugins/

Accessing :
-----------
You should be able to create a WebFolder or to open from MSOffice using :
 - http://server:8080/nuxeo/ : to access directly nuxeo
 - http://server:8080/ : to access directly server root


About VirtualHosting 
====================
For general information about Nuxeo Virtual Hosting, please see :
http://doc.nuxeo.org/5.2/books/nuxeo-book/html-single/index-admin.html#admin-virtualhosting

Because WSS has specific URL managament, it requires additionnal reverse proxy configuration, you need to forward all _vti call to the root to Nuxeo server.

 ProxyPass /nuxeo/ http://127.0.0.1:8080/nuxeo/
 ProxyPass /_vti_bin/ http://127.0.0.1:8080/_vti_bin/
 ProxyPass /_vti_inf.html http://127.0.0.1:8080/_vti_inf.html

 ProxyPassReverse /nuxeo/ http://127.0.0.1:8080/nuxeo/
 ProxyPassReverse /_vti_bin/ http://127.0.0.1:8080/_vti_bin/
 ProxyPassReverse /_vti_inf.html http://127.0.0.1:8080/_vti_inf.html


Deployment in Tomcat 6 
======================

Update of Root WebApp
---------------------

Edit webapps/ROOT/WEB-INF/web.xml 
  <filter>
       <display-name>WSS ROOT Filter</display-name>
       <filter-name>WSSROOTFilter</filter-name>
       <filter-class>org.nuxeo.wss.servlet.WSSFilter</filter-class>
       <init-param>
          <param-name>org.nuxeo.wss.rootFilter</param-name>
          <param-value>/nuxeo</param-value>
       </init-param>
  </filter>
  <filter-mapping>
        <filter-name>WSSROOTFilter</filter-name>
        <url-pattern>/*</url-pattern>
  </filter-mapping>

Add webapps/ROOT/META-INF/context.xml
  <?xml version="1.0" encoding="ISO-8859-1"?>
  <Context path="/" crossContext="true">
  </Context>

Add to tomacat lib :
 - commons-lang-2.3.jar
 - commons-logging.jar
 - freemarker-2.3.11.jar
 - log4j.jar
 - nuxeo-generic-wss-handler-5.4.0-SNAPSHOT.jar

Update nuxeo webapp :
---------------------
Just drop the backend jar into bundles directory





