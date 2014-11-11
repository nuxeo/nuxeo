<?xml version="1.0"?>

<#assign logic = Context.tail() />

<resource name="${name}" >

<#list objectInfo.attributes as attributeInfo>
  <attribute name="${attributeInfo.name}" value="${logic.getObjectAttribute(objectName, attributeInfo)}"/>
</#list>


</resource>
