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
    <h1><a href="${Root.getPath()}/${bank}/style/view">style</a> &gt; <a href="${Root.getPath()}/${bank}/style/${collection}/view">${collection}</a> ${resource?replace('.css', '')}</h1>
    <pre style="visibility: hidden" class="brush: css; toolbar: false">
    ${content}
    </pre>
  </@block>

</@extends>
