<?xml version="1.0"?>
<component name="${component.id}.override">

  <require>${component.id}</require>
  <!--
  ${component.documentation}
  -->

<#if extension??>
  <extension target="${extension.target}" point="${extension.point}">

  </extension>
</#if>
</component>
