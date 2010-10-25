<@extends src="base.ftl">

  <@block name="title">
    Internal Server Error
  </@block>

  <@block name="content">
  
<h1>Internal Server Error</h1>
<p>An error has occured</p>
<pre style="font: 11px serif; height: 300px; overflow: scroll; width: 800px; background-color: #ffc; border: 1px solid #999; padding: 5px">
${stacktrace}
</pre>

  </@block>

</@extends>
