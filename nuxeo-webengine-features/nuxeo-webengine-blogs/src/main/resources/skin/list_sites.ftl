
<@theme>
<@block name="content">

Blogs available
<hr>
<#list sites as s>
<a href="${This.path}/${s.href}"> ${s.name?xml} </a>
<br>
</#list>

</@block>
</@theme>
