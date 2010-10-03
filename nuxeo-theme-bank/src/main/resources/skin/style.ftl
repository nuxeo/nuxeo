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
    <h1>Style: ${resource?replace('.css', '')}</h1>

    <#if action = 'view'>
    <div class="actionBar">
    <a href="${Root.getPath()}/${bank}/style/${collection}/${style}${resource}/edit">
    <img src="${basePath}/theme-banks/skin/img/edit.png" />
    Edit</a>
    </div>
    <pre class="brush: css; toolbar: false">
    ${content}
    </pre>
    </#if>

    <#if action = 'edit'>
    <form>
      <div>
        <textarea style="width: 100%; height: 300px" class="" name="css">${content}</textarea>
        <div>
        <button>SAVE</button>
        <button>Cancel</button>
        </div>
      </div>
    </form>
    </#if>

  </@block>

</@extends>
