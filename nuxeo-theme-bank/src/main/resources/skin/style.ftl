<@extends src="base.ftl">

  <@block name="title">
      ${collection} ${resource}
  </@block>

  <@block name="stylesheets">
    <link type="text/css" rel="stylesheet" href="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shCore.css"/>
    <link type="text/css" rel="stylesheet" href="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shThemeDefault.css"/>
  </@block>

  <@block name="header_scripts">
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shCore.js"></script>
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shBrushCss.js"></script>
    <script type="text/javascript">
        SyntaxHighlighter.all();
    </script>
  </@block>

  <@block name="content">
    <h1>Style: ${resource?replace('.css', '')}
      <a style="float: right" href="${Root.getPath()}/${bank}/${collection}/style/${resource}/${action}">Refresh</a>
      <#if Root.isAdministrator()>
        <#if action = 'edit'>
          <a style="float: right; margin-right: 5px"
             href="${Root.getPath()}/${bank}/${collection}/style/${resource}/view">Cancel</a>
        <#else>
          <a style="float: right; margin-right: 5px"
             href="${Root.getPath()}/${bank}/${collection}/style/${resource}/edit">Edit</a>
        </#if>
      </#if>

    </h1>

    <#if action = 'view'>

<pre class="brush: css; toolbar: false">
${content}
</pre>
    </#if>

    <#if action = 'edit'>
    <#assign redirect_url="${Root.getPath()}/${bank}/${collection}/style/${resource}/view" />
    <form action="${Root.getPath()}/${bank}/manage/saveCss" method="post">
      <div>
        <textarea style="width: 100%; height: 300px" class="" name="css">${content}</textarea>
        <input type="hidden" name="collection" value="${collection}" />
        <input type="hidden" name="resource" value="${resource}" />
        <input type="hidden" name="redirect_url" value="${redirect_url?replace(' ', '%20')}" />
      </div>
      <p>
        <button>SAVE</button>
      </p>
    </form>
    </#if>

  </@block>

</@extends>
