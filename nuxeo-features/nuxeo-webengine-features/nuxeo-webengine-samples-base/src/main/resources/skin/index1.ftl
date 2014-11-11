<@extends src="base.ftl">
  <@block name="title">Templating samples</@block>
  <@block name="header">
    <#if name>
    Hello ${name}!
    <#else>
    Hello World!
    </#if>
  </@block>
  <@block name="content">
    This is the <i>index1</i> skin.
  </@block>
</@extends>
