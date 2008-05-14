<@extends src="/default/base.ftl">
<@block name="content">
<h2>${This.document.title}</h2>

<h3>I am a Wiki Page</h3>

<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

<p>
<@transform name="wiki">${This.document.wikiPage.content}</@transform>
</p>

</@block>
</@extends>
