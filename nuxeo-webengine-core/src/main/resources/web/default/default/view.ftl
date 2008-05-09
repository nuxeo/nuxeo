<@extends src="/default/base.ftl">
<@block name="content">
<h2>${this.document.title}</h2>

<hr/>
<A href="${this.urlPath}@@edit">Edit</A><BR/>
<hr/>

The root path: ${context.firstObject.path}

</@block>
</@extends>