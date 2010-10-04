<@extends src="base.ftl">

  <@block name="title">Action bar</@block>

  <@block name="content">

  <div class="actionBar">
    <span style="float: left; font-weight: bold; color: #fff">Nuxeo Theme Bank</span>
    <#if !Context.principal>
      <a href="${Root.getPath()}/login" target="main"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log in</a>
    <#else>
      <span style="margin-right: 15px">You are logged in as: <strong>${Context.principal}</strong></span>
      <a href="${Root.getPath()}/login" target="main"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log out</a>
    </#if>
  </div>

  </@block>

</@extends>