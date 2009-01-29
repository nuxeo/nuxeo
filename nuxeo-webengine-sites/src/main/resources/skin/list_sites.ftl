
<@theme>
<@block name="content">

Sites available
<hr>
<#list sites as s>
<a href="${This.path}/${s.href}"> ${s.name} </a>
<br>
</#list>

</@block>
</@theme>