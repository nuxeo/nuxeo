<html>
<head>
<title>Nuxeo Shell</title>
</head>
<body>
    
    <script src="http://www.java.com/js/deployJava.js"></script>
    <script> 
        var attributes = { code:'org.nuxeo.ecm.shell.swing.ShellApplet',  width:800, height:600} ; 
        var parameters = {jnlp_href: '${This.path}/shell.jnlp', 
            host: '${Context.baseUrl}/automation', user: '${Context.principal.name}'} ; 
        deployJava.runApplet(attributes, parameters, '1.6'); 
    </script>
    
</body>
</html>
