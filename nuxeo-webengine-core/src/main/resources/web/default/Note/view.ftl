<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.document.title}</h2>

<hr/>
<A href="${this.absolutePath}@@edit">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${this.document.note.note}</@transform>
</p>

</@block>
</@extends>