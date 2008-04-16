<@extends src="/templates/Folder/view.ftl">
<@block name="content">
<h2>${this.title}</h2>

<hr/>
<A href="${this.docURL}?render_mode=EDIT">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${this.note.note}</@transform>
</p>

</@block>
</@extends>