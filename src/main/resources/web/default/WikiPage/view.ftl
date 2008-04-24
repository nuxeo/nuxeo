<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.title}</h2>

<h3>I am a Wiki Page</h3>

<hr/>
<A href="${this.docURL}@@edit">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${this.wikiPage.content}</@transform>
</p>

</@block>
</@extends>
