<@extends src="base.ftl">
<@block name="content">
<h2>${This.document.title}</h2>

<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

<p>
<@wiki>${This.document.note.note}</@wiki>
</p>

</@block>
</@extends>