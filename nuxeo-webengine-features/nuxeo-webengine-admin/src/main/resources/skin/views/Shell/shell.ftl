<html>
<head>
<title>Nuxeo Shell</title>
</head>
<body>
  <#assign automationURL>${Context.serverURL}${Context.request.contextPath}${Context.request.servletPath}/automation</#assign>
  
  <applet id="nxshell" width="800" height="600" archive="${Root.URL}/shell.jar" code="org.nuxeo.shell.swing.ShellApplet">
    <param name="codebase_lookup" value="false">
    <param name="jnlp_href" value="${Root.URL}/applet.jnlp">
    <param name="host" value="${automationURL}">
    <#if Context.principal??> 
    <param name="user" value="${Context.principal.name}">
    </#if>
  </applet>
  
  
   
  <!--
  <script src="http://www.java.com/js/deployJava.js"></script>
  <script> 
    var attributes = {archive:'${Root.URL}/shell.jar', code:'org.nuxeo.shell.swing.ShellApplet', width:800, height:600} ; 
    var parameters = {jnlp_href: '${Root.URL}/applet.jnlp', host: '${automationURL}' 
    <#if Context.principal??> 
    , user: '${Context.principal.name}'
    </#if>    
    }; 
    deployJava.runApplet(attributes, parameters, '1.6'); 
  </script>
  -->
</body>
</html>
