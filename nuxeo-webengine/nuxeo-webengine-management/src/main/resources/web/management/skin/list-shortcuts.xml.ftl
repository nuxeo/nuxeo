<?xml version="1.0"?>
<#assign logic = Context.tail()>
<#assign names = logic.getShortcutsName() />
<resources>
<#list names as name>
  <resource name="${name}" path="${logic.getPath(name)}"/>
</#list>
</resources>

