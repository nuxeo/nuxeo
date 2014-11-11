<@extends src="base.ftl">
<@block name="title">Wikis</@block>
<@block name="header"><h1><a href="${basePath}">Nuxeo WebEngine</a></h1></@block>
<@block name="content">

<div id="mainContentBox">
The following wikis are available:
<ul>
  <#list wikis as wiki>
    <li id="${wiki.id}">
      <a href="${This.path}/${wiki.id}">${wiki.title}</a>
    </li>
  </#list>
</ul>
</div>

</@block>

</@extends>
