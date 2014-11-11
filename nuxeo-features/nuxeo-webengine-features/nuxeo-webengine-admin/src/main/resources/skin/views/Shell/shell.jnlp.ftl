<?xml version="1.0" encoding="UTF-8"?>
<#assign automationURL>${Context.serverURL}${Context.request.contextPath}${Context.request.servletPath}/automation</#assign>
<#assign codebase>${Root.URL}</#assign>
<jnlp spec="1.0+" codebase="${codebase}" href="shell.jnlp">
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
        <jar href="shell.jar" main="true" />

    </resources>
    <application-desc main-class="org.nuxeo.shell.swing.ShellFrame">
      <#if Context.principal??> 
      <argument>-u ${Context.principal.name}</argument>
      </#if>
      <argument>${automationURL}</argument>
    </application-desc>
    <update check="background"/>
</jnlp>
