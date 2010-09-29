<@extends src="base.ftl">

  <@block name="title">Action bar</@block>

  <@block name="content">

  <div class="actionBar">
    <#if !Context.principal>
      <a href="${Root.getPath()}/login" target="main"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log in</a>
    <#else>
      You are logged in as: <strong>${Context.principal}</strong> <a href="${Root.getPath()}/login" target="main"><img src="${basePath}/theme-banks/skin/img/login.png" /> Log out</a>
    </#if>
  </div>

  </@block>

</@extends>