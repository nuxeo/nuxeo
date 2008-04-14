jbossctl script

Requirements :
        copy jbossctl into $JBOSS/bin/
        copy jbossctl.conf.sample to $JBOSS/bin/jbossctl.conf (optional; only needed to change default values, or to use debug mode with startd)
        org.nuxeo.runtime.osgi.OSGiRuntimeService must be logged with INFO level

Settings :
        edit jbossctl.conf if you want to change default values

Usage :
        jbossctl (start|stop|startd|restart|status|tail|tailf|info|help)
