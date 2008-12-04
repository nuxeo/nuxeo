#!/bin/sh

cat <<EOF > .classpath
<?xml version="1.0" encoding="utf-8" ?>
<classpath>
   <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
   <classpathentry kind="src" path="src"/>
   <classpathentry kind="src" path="../nuxeo-webengine-gwt-core"/>
   <classpathentry kind="lib" path="$GWT_HOME/gwt-user.jar"/>
   <classpathentry kind="lib" path="$GWT_HOME/smartgwt.jar"/>
   <classpathentry kind="var" path="JUNIT_HOME/junit.jar"/>   
   <classpathentry kind="output" path="bin"/>
</classpath>
EOF

cat <<EOF > .project
<?xml version="1.0" encoding="utf-8" ?>
<projectDescription>
   <name>nuxeo-webengine-gwt-ui</name>
   <comment>nuxeo-webengine-gwt-ui project</comment>
   <projects/>
   <buildSpec>
       <buildCommand>
           <name>org.eclipse.jdt.core.javabuilder</name>
           <arguments/>
       </buildCommand>
   </buildSpec>
   <natures>
       <nature>org.eclipse.jdt.core.javanature</nature>
   </natures>
</projectDescription>
EOF
