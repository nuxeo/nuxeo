<@extends src="base.ftl">
<@block name="header"><h1><a href="${appPath}">Error</a></h1></@block>
<@block name="content">

<h1>500 - Internal Server Error</h1>

<p>
Server failed to handle request. Here is the stack trace:
</p>
<pre>
${stacktrace}
</pre>

</@block>
</@extends>
