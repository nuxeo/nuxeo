<@extends src="base.ftl">

  <@block name="content">

  <#include "includes/header.ftl">

  <div>
    <div class="denied"><i class="icon-unhappy"></i>${Context.getMessage("easyshare.label.expired")}</div>
  </div>

  </@block>

</@extends>
