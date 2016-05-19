<#if Root.currentDistribution!=null>

<#assign navPoint=Root.getNavigationPoint()/>

<#if !Root.isEmbeddedMode()>

<div class="nav-box">
<ul>
  <li <#if navPoint=="listExtensionPoints">class="selected"</#if> >
    <a class="extensions" href="${Root.path}/${distId}/listExtensionPoints">Extension points</a>
  </li>

  <li <#if navPoint=="listContributions">class="selected"</#if> >
  <a class="contributions" href="${Root.path}/${distId}/listContributions">Contributions</a>
  </li>

  <li <#if navPoint=="listServices">class="selected"</#if> >
    <a class="services" href="${Root.path}/${distId}/listServices">Services</a>
  </li>

  <li <#if navPoint=="listOperations">class="selected"</#if> >
    <a class="operations" href="${Root.path}/${distId}/listOperations">Operations</a>
  </li>

  <li <#if navPoint=="listComponents">class="selected"</#if> >
    <a class="components" href="${Root.path}/${distId}/listComponents">Components</a>
  </li>

  <li <#if navPoint=="listBundles">class="selected"</#if> >
    <a class="bundles" href="${Root.path}/${distId}/listBundles">Bundles</a>
  </li>

  <#if Root.showSeamComponent() >
    <li <#if navPoint=="listSeamComponents">class="selected"</#if> >
    <a class="seam" href="${Root.path}/${distId}/listSeamComponents">Seam components</a>
    </li>
  </#if>

</ul>
</div>

<div class="nav-box display-none">
<h4> Browse by hierarchy </h4>
</#if> <#-- if embedded -->

<div id="treeControler"></div>

<#macro tree id url="${Root.path}/${distId}/tree" root="/">
  <script type="text/javascript">
  var currentSelectedTreeId='${Context.request.getAttribute("tree-last-path")}';
  var anonymousTree=false;
  <#if Context.getPrincipal().isAnonymous()>
   anonymousTree = true;
  </#if>
  $(document).ready(function(){
    $("#${id}").treeview({
      url: "${url}",
      root: "${root}",
      animated: "fast"
    });

  });
  </script>
  <ul id="${id}" class="filetree">
  </ul>

</#macro>

<@tree id="myTree" root="/"/>
</div>

</#if>
