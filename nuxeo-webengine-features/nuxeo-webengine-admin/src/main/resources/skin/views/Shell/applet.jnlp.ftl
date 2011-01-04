<?xml version="1.0" encoding="UTF-8"?>
<jnlp spec="1.0+">
    <information>
        <title>Nuxeo Shell</title>
        <vendor>Nuxeo</vendor>
    </information>
    <security>
        <all-permissions/>
    </security>
    <resources>
        <!-- Application Resources -->
        <j2se version="1.5+"
              href="http://java.sun.com/products/autodl/j2se" />
        <jar href="${Root.URL}/shell.jar" main="true" />
    </resources>
    <applet-desc
         name="Nuxeo Shell"
         main-class="org.nuxeo.shell.swing.ShellApplet"
         width="800"
         height="600">
     </applet-desc>
     <update check="background"/>
</jnlp>
