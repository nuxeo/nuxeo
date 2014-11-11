<#macro webPageTabs>

  <link rel="stylesheet" href="${skinPath}/css/webengine.css" type="text/css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
  <script>
    $(document).ready(function(){
      $("#webpage-actions > ul").tabs();
    });
  </script>

  <div id="webpage-actions">
    <ul>
      <#list This.getLinks("WEBPAGE_ACTIONS") as link>
        <li><a href="${link.getCode(This)}" title="${link.id}"><span>${Context.getMessage(link.id)}</span></a></li>
      </#list>
    </ul>
  </div>

</#macro>
