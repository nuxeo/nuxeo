<@extends src="base.ftl">

<@block name="title">Registered Services</@block>
  
<@block name="content">
  
<#assign names = Context.tail().getServices() />

<table class="itemListing">
  <thead>
    <tr>
        <th>Name</th>
    </tr>
  </thead>
  <tbody>
<#list names as name>
    <tr>
        <td><a href="${Context.getModulePath()}/${name.canonicalName}">${name.canonicalName}</a></td>
    </tr>
</#list>
  </tbody>
</table>

</@block>

</@extends>
