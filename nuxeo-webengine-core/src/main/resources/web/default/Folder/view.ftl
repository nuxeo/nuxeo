<@extends src="/default/base.ftl">
<@block name="content">
<h2>${This.title}</h2>

<h3>I am a folder</h3>

<p>${french}</p>

<#--
<#list Context.search("SELECT * FROM Document") as entry>
        <li>${entry.title}</li>
        </#list>
-->
aaaaaaaaaaaaaa
<ul>
 <#list Context.search("SELECT * FROM Document") as doc>
<li> ${doc.title} </li>
</#list>
</ul>
</@block>
</@extends>
