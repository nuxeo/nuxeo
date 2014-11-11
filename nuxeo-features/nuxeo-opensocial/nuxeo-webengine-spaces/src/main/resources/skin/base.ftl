<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<@theme>
  <@block name="header_scripts">
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />


    <!-- //TODO: put this in a theme resource -->
    <script type="text/javascript" src="${skinPath}/script/loading.js"></script>
  </@block>

  <@block name="title">
    <#if Document >${Document.title}</#if>
  </@block>

</@theme>