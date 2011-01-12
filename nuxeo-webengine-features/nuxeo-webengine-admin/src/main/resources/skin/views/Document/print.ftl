<#import "common/util.ftl" as base/>
<html>
  <head>
    <title>${Root.document.title} :: ${This.document.title} :: print preview</title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <link rel="stylesheet" href="${skinPath}/css/print.css" type="text/css" media="print" charset="utf-8">
    <link rel="stylesheet" href="${skinPath}/css/print_version.css" type="text/css" media="screen" charset="utf-8">
  </head>

  <body>

<div class="printButton">
  <form>
    <input type="button" value=" Print! " onclick="window.print();return false;" />
  </form>
</div>

<div class="closeWindow">
  <form>
    <input type="button" value=" Close this window " onclick="self.close();" />
  </form>
</div>

<hr/>

<div class="wikiName">URL: <span>${This.path}</span></div>

<hr/>

<div id="entry-print">
  <@block name="print-content">
  <h1>${Document.title}</h1>

  <div id="entry-content">
    <#include "@print_page"/>
  </div>

  </@block>
</div>

<hr/>

<#if Document.modified>
<div class="byline">Last modified on ${Document.modified?datetime}< by ${Document.author}</div>
<hr/>
</#if>

<hr/>

<div class="printButton">
  <form>
    <input type="button" value=" Print! " onclick="window.print();return false;" />
  </form>
</div>

<div class="closeWindow">
  <form>
    <input type="button" value=" Close this window " onclick="self.close();" />
  </form>
</div>


  </body>
</html>