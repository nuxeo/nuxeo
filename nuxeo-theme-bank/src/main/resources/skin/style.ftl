<@extends src="base.ftl">

  <@block name="title">
      ${collection} ${resource}
  </@block>

  <@block name="stylesheets">
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shCore.css"/>
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shThemeDefault.css"/>
  </@block>
  
  <@block name="header_scripts">
    <script type="text/javascript" src="${skinPath}/scripts/syntaxHighlighter/shCore.js"></script>
    <script type="text/javascript" src="${skinPath}/scripts//syntaxHighlighter/shBrushCss.js"></script>
    <script type="text/javascript">
        SyntaxHighlighter.all();
    </script>
  </@block>
    
  <@block name="content">

    <h1>${collection} ${resource}</h1>
    <pre style="visibility: hidden" class="brush: css; toolbar: false">
    ${content}
    </pre>

  </@block>

</@extends>