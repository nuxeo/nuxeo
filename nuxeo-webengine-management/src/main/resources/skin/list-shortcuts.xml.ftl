<?xml version="1.0"?>
<#assign logic = Context.tail()>

<resources>
<#list shortcuts as name>
  <resource name="${name}" path="${logic.getPath(name)}"/>
</#list>
</resources>

