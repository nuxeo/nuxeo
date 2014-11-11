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

* copy jbossctl.conf.sample to jbossctl.conf and edit it if you want to change default values

* edit bind.conf if you want to bind JBoss on a specific IP (by default, only localhost is binded; 
  set BINDHOST=0.0.0.0 to bind all available addresses)

Usage
-----

  jbossctl (start|stop|startd|restart|status|tail|tailf|info|help)

  
Annexe
------

* For Solaris sytems, use jbossctl.solaris