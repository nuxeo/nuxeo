<@extends src="/default/base.ftl">
<@block name="content">
<h2>${This.document.title}</h2>

<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

The root path: ${Context.firstObject.path}

</@block>
</@extends>