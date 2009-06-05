===============
jbossctl script
===============

Requirements
------------

* Copy jbossctl into $JBOSS/bin.

* Copy jbossctl.conf.sample to $JBOSS/bin/jbossctl.conf (optional: only
  needed to change default values, or to use debug mode with startd).

* Copy bind.conf into $JBOSS/bin.

* log4j must log messages with category
  org.nuxeo.runtime.osgi.OSGiRuntimeService at INFO level for the FILE
  appender (see server/default/conf/log4j.xml).


Settings
--------

* edit jbossctl.conf if you want to change default values

* edit bind.conf if you want to bind JBoss on a specific IP

Usage
-----

  jbossctl (start|stop|startd|restart|status|tail|tailf|info|help)
