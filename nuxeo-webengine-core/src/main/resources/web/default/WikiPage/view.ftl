<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.document.title}</h2>

<h3>I am a Wiki Page</h3>

<hr/>
<A href="${this.absolutePath}@@edit">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${this.document.wikiPage.content}</@transform>
</p>

</@block>
</@extends>
