<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.title}</h2>

<hr/>
<A href="${this.docURL}/edit.ftl">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${this.note.note}</@transform>
</p>

The root path: ${root.path}

</@block>
</@extends>