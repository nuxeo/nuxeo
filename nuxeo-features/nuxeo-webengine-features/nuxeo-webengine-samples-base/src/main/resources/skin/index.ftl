<@extends src="base.ftl">
  <@block name="title">Web Engine Samples</@block>
  <@block name="header">Samples Index</@block>
  <@block name="content">
  You can see how working with web engine following the samples about
    <ol>
      <li><a href="${This.path}/hello">handling requests</a></li>
      <li><a href="${This.path}/templating">working with templates</a></li>
      <li><a href="${This.path}/basics">understanding web engine object model</a></li>
      <li><a href="${This.path}/documents">browsing documents</a></li>  
    </ol>
  </@block>
</@extends>
