<?xml version="1.0"?>
<#assign logic = Context.tail()>
<#assign names = logic.getShortNames() />
<resources>
<#list names as name>
  <resource name="${name}" qname="${logic.getQualifiedName(name)}" path="${logic.getPath(name)}"/>
</#list>
</resources>

