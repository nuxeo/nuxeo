==========================
Nuxeo Platform Layout Demo
==========================

Addon presenting layouts and widgets in a show-case like web site.

The application is visible at http://localhost:8080/nuxeo/layoutDemo 

Selenium tests also ensure that all standard widget types are working ok.


Install
=======

1. Put generated jar nuxeo-platform-forms-layout-demo to your
   nxserver/bundles directory
2. Add the following to you nuxeo.conf file:

    nuxeo.server.declare_datasources=false

3. Start the server.

Development
===========

Add a new widget type
---------------------

To add a new widget type to the application, you need to:
- add a xhtml template in nuxeo.war/layoutDemo/demoWidgets
- declare this template with a view id in OSGI-INF/deployment-fragment.xml
- provide the widget sample xml configuration in OSGI-INF/demo and
  reference it in the MANIFEST
- configure the widget type in OSGI-INF/layout-demo-contrib.xml
 
Launch selenium tests
---------------------

Run:

    $ mvn clean install -Pitest
