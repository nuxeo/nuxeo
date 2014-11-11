===============
jbossctl script
===============

Requirements
------------

* depends on $JBOSS/bin/run.sh

* to be used from $JBOSS/bin/

* log4j must log messages with category org.nuxeo.runtime.osgi.OSGiRuntimeService at INFO level 
  for the FILE appender (see server/default/conf/log4j.xml).

Settings
--------

* edit jbossctl.conf if you want to change default values

* edit bind.conf if you want to bind JBoss on a specific IP 
  By default, it is configured to bind all available addresses (0.0.0.0)

Usage
-----

  jbossctl (start|stop|startd|restart|status|tail|tailf|info|help)

  
Annexe
------

* For Solaris sytems, use jbossctl.solaris