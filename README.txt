==========================
Nuxeo Platform Layout Demo
==========================

Addon presenting layouts and widgets in a show-case like web site.

The application is visible at http://localhost:8080/nuxeo/layoutDemo 

Selenium tests also ensure that all standard widget types are working ok.


Install
=======

Setup properties in a file called "build.properties" according to you needs
and run "ant deploy" to deploy on a JBoss with nuxeo installed.


Development
===========

Add a new widget type
---------------------

To add a new widget type to the application, you need to:
- add a xhtml template in nuxeo.war/layoutDemo/widgets
- declare this template with a view id in OSGI-INF/deployment-fragment.xml
- provide the widget sample xml configuration in OSGI-INF/demo and
  reference it in the MANIFEST
- configure the widget type in OSGI-INF/layout-demo-contrib.xml 
