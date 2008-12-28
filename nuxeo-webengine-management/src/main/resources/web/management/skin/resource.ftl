<@extends src="base.ftl">
  <@block name="title">Index</@block>
  <@block name="header">
    Zoo!
  </@block>
  
  <@block name="content">
  
  <#assign management = Context.tail() />
  <#assign objectName = management.getObjectName() />
  <#assign objectInfo = management.getObjectInfo() />

  <div>
    <pre>${objectName}</pre>
    <ul> 
<#list objectInfo.attributes as attributeInfo>
      <li>${attributeInfo.name} ${management.getObjectAttribute(attributeInfo)}</li>
</#list>
    </ul>
  </div>
  </@block>

  
</@extends>
