<@extends src="/default/base.ftl">
<@block name="content">
<h2>${This.title}</h2>

<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${This.note.note}</@transform>
</p>

</@block>
</@extends>