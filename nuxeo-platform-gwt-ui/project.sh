#!/bin/sh

PROJECT_NAME="nuxeo-platform-gwt-ui"
PROJECT="nuxeo-platform-gwt-ui"
MAIN_HTML="org.nuxeo.ecm.platform.gwt.SmartClient/Main.html"

cat <<EOF > .classpath
<?xml version="1.0" encoding="utf-8" ?>
<classpath>
   <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
   <classpathentry kind="src" path="src"/>
   <classpathentry kind="src" path="../nuxeo-platform-gwt-core"/>
   <classpathentry kind="lib" path="$GWT_HOME/gwt-user.jar"/>
   <classpathentry kind="lib" path="$GWT_HOME/smartgwt.jar"/>
   <classpathentry kind="var" path="JUNIT_HOME/junit.jar"/>   
   <classpathentry kind="output" path="bin"/>
</classpath>
EOF

cat <<EOF > .project
<?xml version="1.0" encoding="utf-8" ?>
<projectDescription>
   <name>${PROJECT_NAME}</name>
   <comment>${PROJECT_NAME} project</comment>
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


cat <<EOF > ${PROJECT_NAME}.launch
<?xml version="1.0" encoding="UTF-8"?>
<launchConfiguration type="org.eclipse.jdt.launching.localJavaApplication">
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
<listEntry value="/${PROJECT_NAME}"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
<listEntry value="4"/>
</listAttribute>
<listAttribute key="org.eclipse.jdt.launching.CLASSPATH">
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry containerPath=&quot;org.eclipse.jdt.launching.JRE_CONTAINER&quot; javaProject=&quot;${PROJECT_NAME}&quot; path=&quot;1&quot; type=&quot;4&quot;/&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry id=&quot;org.eclipse.jdt.launching.classpathentry.defaultClasspath&quot;&gt;&#10;&lt;memento exportedEntriesOnly=&quot;false&quot; project=&quot;${PROJECT_NAME}&quot;/&gt;&#10;&lt;/runtimeClasspathEntry&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry internalArchive=&quot;/nuxeo-platform-gwt-ui/src&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry internalArchive=&quot;/nuxeo-platform-gwt-core/src&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry internalArchive=&quot;/${PROJECT_NAME}/src&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#10;"/>
</listAttribute>
<booleanAttribute key="org.eclipse.jdt.launching.DEFAULT_CLASSPATH" value="false"/>
<stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="com.google.gwt.dev.GWTShell"/>
<stringAttribute key="org.eclipse.jdt.launching.PROGRAM_ARGUMENTS" value="-out &quot;\${container_loc}/target/www&quot; ${MAIN_HTML}"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="${PROJECT_NAME}"/>
<stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="-Xmx256M"/>
</launchConfiguration>
EOF


cat <<EOF > tomcat/webapps/ROOT/WEB-INF/web.xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app>

  <servlet>
    <servlet-name>shell</servlet-name>
    <servlet-class>com.google.gwt.dev.shell.GWTShellServlet</servlet-class>
  </servlet>
  
  <servlet-mapping>
    <servlet-name>shell</servlet-name>
    <url-pattern>/*</url-pattern>
  </servlet-mapping>

  <servlet>
    <servlet-name>redirect</servlet-name>
    <servlet-class>org.nuxeo.ecm.platform.gwt.debug.RedirectServlet</servlet-class>
  </servlet>
  
  <servlet-mapping>
    <servlet-name>redirect</servlet-name>
    <url-pattern>/redirect/*</url-pattern>
  </servlet-mapping>

</web-app>

EOF
