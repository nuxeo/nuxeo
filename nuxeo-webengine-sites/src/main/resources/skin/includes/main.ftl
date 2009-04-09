<#macro main>
  
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
  	
  <script>
  
    $(document).ready(function(){
    $("#webpage-actions > ul").tabs();
    });
  </script>
  
  <div id="webpage-actions">
    <ul>
      <#list This.getLinks("SITE_ACTIONS") as link>
        <li><a href="${link.getCode(This)}" title="${link.id}"><span>${Context.getMessage(link.id)}</span></a></li>
      </#list>
    </ul>
  </div>

</#macro>