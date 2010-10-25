<@extends src="base.ftl">

  <@block name="title">Action bar</@block>

  <@block name="content">

  <div class="actionBar">
    <a style="float: left;" target="main" href="${Root.getPath()}/banks">Nuxeo Theme Bank</a>
    <#if !Context.principal>
      <a href="${Root.getPath()}/session/login" target="_parent"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log in</a>
    <#else>
      <span style="margin-right: 15px">You are logged in as: <strong>${Context.principal}</strong></span>
      <a href="${Root.getPath()}/session/login" target="_parent"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log out</a>
    </#if>
  </div>

  </@block>

</@extends>
