<?xml version="1.0"?>
<component name="${component.id}.override">

  <require>${component.id}</require>

<#if component.documentation?has_content>
<!--
${component.documentation}
-->
</#if>
<#if contribution??>
  ${contribution.xml}
</#if>

</component>
