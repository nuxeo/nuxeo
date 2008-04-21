<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.title}</h2>

<h3>I am a folder</h3>

<#--
<#list query("SELECT * FROM Document") as entry>
        <li>${entry.title}</li>
        </#list>
-->
<ul>
 <#list session.getChildren(this.document.ref) as doc>
<li> ${doc.title} </li>
<br/>
</#list>
</ul>
</@block>
</@extends>
