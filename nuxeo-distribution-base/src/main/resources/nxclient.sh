#!/bin/sh
################################################
# Nuxeo Framework Launcher
################################################
# The following options may be used:
# -debug - launch Java in debug mode.
#          By default the address 127.0.0.1:8788 is used and suspend mode is set to true
# The following Java System properties may be used:
# -Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.loader.NuxeoRMIClassLoader
#     to replace default RMI class loader which is not too robust when dealing with malformed URLs.
#     (You can generate malformed URLs by using java.io.File.toURL() method)
#     The Nuxeo version is also removing inutile information sent over the network at each RMI call
# -Dsun.lang.ClassLoader.allowArraySyntax=true
#     useful when running java5 code on java6
# There are also a set of properties that may be used but it is recommended to use framework options
# to control this:
# -Dorg.nuxeo.app.home
# -Dorg.nuxeo.app.web
# -Dorg.nuxeo.app.config
# -Dorg.nuxeo.app.data
# -Dorg.nuxeo.app.log
# These options are overrited by the corresponding framework options.
# See fthe framework options for more details
#
# The java command used is of the form:
# java [java_options] -jar launcher.jar framework.jar/MainClass[:mainMethod] frameworkClassPath [framework_options]
# The frameworkClassPath is a class path expression that use ':' to separate classpath entries.
# You may use classpath entries that ends in a '/.' to specify all files inside the specified entry.
# Example: osgi.jar:lib/.:bundles/.
# The classpath is used to resolve classes but also to discover OSGi bundles. Any OSGi bundle on the classpath will
# be deployed as an OSGi bundle
#
# The framework options are:
# -clear
#     to start without cache as the first time the application was started. The cache will be rebuild
# -console
#     display a console after starting the framework. The nuxeo console should be available on the framework classpath
# -bundles=bundle1.jar@1:bundle2.jar@2:.
#     a list of bundles to start initialy. Each bundle may be terminated by a @START_LEVEL
#     which is specifying when the bundle should be started. If no start level is specified the default is 1.
#     The 0 start level is set before loading the system bundle. After loading it the start level is set to 1.
#     After all bundles in start level 1 are started, the start level 2 is entered and the bundles discovered
#     in classpath will be loaded.
#     If you specify bundles in start level 2 they will be loaded before the discovery is started.
#     After discovery is finished start level 3 is entered.
# -home
#     the home directory. Defaults to the current directory
# -log
#     the log directory. Defaults to ${home}/log
# -config
#     the config directory. Defaults to ${home}/config
# -data
#     the data directory. Defaults to ${home}/data
# -web
#     the web root directory. Defaults to ${home}/web
# -extractNestedJars
#     extract nested jars from bundles. It's ignored when cache is used
# -scanForNestedJars
#     whether to scan for nested jars. It's ignored when cache is used.
#     This is by default true to be able to handle malformed JARs that doesn't have class path references in manifest
#
###############################################
#
# Author: Bogdan Stefanescu <bs@nuxeo.com>
#
################################################

JAVA_OPTS="-Djava.rmi.server.RMIClassLoaderSpi=org.nuxeo.runtime.launcher.NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true"
JAVA_OPTS="$JAVA_OPTS -Dderby.system.home=data/derby" 
#JAVA_OPTS="$JAVA_OPTS -Dorg.nuxeo.runtime.1.3.3.streaming.port=3233"

CMD_ARGS="$@"
DEV_OPTS=""
if [ "x$1" = "x-dev" ] ; then
# remove dev parameter
    shift 1
    JAVA_OPTS="$JAVA_OPTS -Dorg.nuxeo.dev=true -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=n"
    DEV_OPTS="-clear -console"
fi 

NXC_VERSION=`ls nuxeo-runtime-launcher-*|cut -d"-" -f4- `

#example on how to add external bundles to your environment. Usefull to dev. using IDEs.
#the eclipse plugin is using this option to start webengine.
#POST_BUNDLES="-post-bundles /path/to/your/external/bundle:/path/to/second/bundle:/etc"

java $JAVA_OPTS -jar nuxeo-runtime-launcher-${NXC_VERSION} \
    bundles/nuxeo-runtime-osgi-${NXC_VERSION}/org.nuxeo.osgi.application.Main \
    bundles/.:lib/.:config $POST_BUNDLES -home . $DEV_OPTS "$@"
