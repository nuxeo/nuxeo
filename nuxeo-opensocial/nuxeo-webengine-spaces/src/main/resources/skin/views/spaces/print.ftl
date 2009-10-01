<html>
  <head>
    <title>Impression de la liste des univers </title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
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
    <ul>
      <#list This.universList as univers>
         <li> ${univers.title}</li>
      </#list>
    </ul>
  </@block>
</div>

<hr/>

<div class="byline">Copyright Damien de St Laurent - All rights reserved</div>
<hr/>

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