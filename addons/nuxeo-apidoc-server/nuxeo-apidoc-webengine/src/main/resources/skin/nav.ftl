<#if Root.currentDistribution!=null>

<#assign navPoint=Root.getNavigationPoint()/>

<#if !Root.isEmbeddedMode()>

<p class="explore"><a href="${Root.path}"> See all available distributions </A></p>

<p class="explored"><a href="${Root.path}/${Root.currentDistribution.key}/">${Root.currentDistribution.name} ${Root.currentDistribution.version}</a>
<#if Root.currentDistribution.isLive()>
 (Live)
</#if>
</p>

<div class="nav-box">
  <h4>Browse by category</h4>


<ul>
  <li <#if navPoint=="listOperations">class="selected"</#if> >
    <a href="${Root.path}/${distId}/listOperations">
      Operations
    </a>
    <#if navPoint=="viewOperation">
        <ul><li class="selected">view Operation</li></ul>
      </#if>
  </li>

 <#if Root.currentDistribution.isLive()>
  <li>
    <a href="/nuxeo/api/v1/doc" target="NuxeoREST">
      REST API
    </a>
  </li>
 </#if>

  <li <#if navPoint=="listSeamComponents">class="selected"</#if> >
    <a href="${Root.path}/${distId}/listSeamComponents">
       Seam components
      </a>
    <#if navPoint=="viewSeamComponent">
        <ul><li class="selected">view Seam Component</li></ul>
      </#if>
  </li>
  <li <#if navPoint=="listBundleGroups">class="selected"</#if> >
    <a href="${Root.path}/${distId}/listBundleGroups">
       Bundle groups
      </a>
    <#if navPoint=="viewBundleGroup">
        <ul><li class="selected">view Bundle group</li></ul>
      </#if>
      <ul>
        <li <#if navPoint=="listBundles">class="selected"</#if> >
          <a href="${Root.path}/${distId}/listBundles">
       Bundles
      </a>
          <#if navPoint=="viewBundle">
          <ul><li class="selected">view Bundle</li></ul>
          </#if>

            <li <#if navPoint=="listComponents">class="selected"</#if> >
            <a href="${Root.path}/${distId}/listComponents">
       Components
      </a>
            <#if navPoint=="viewComponent">
            <ul><li class="selected">view Component</li></ul>
            </#if>

            <ul>
            <li <#if navPoint=="listServices">class="selected"</#if> >
            <a href="${Root.path}/${distId}/listServices">
       Services
      </a>
            <#if navPoint=="viewService">
            <ul><li class="selected">view Service</li></ul>
            </#if>
          </li>
           <li <#if navPoint=="listExtensionPoints">class="selected"</#if> >
            <a href="${Root.path}/${distId}/listExtensionPoints">
       Extension points
      </a>
            <#if navPoint=="viewExtensionPoint">
            <ul><li class="selected">view Extension Point</li></ul>
            </#if>
          </li>
           <li <#if navPoint=="listContributions">class="selected"</#if> >
            <a href="${Root.path}/${distId}/listContributions">
       Contributions
      </a>
            <#if navPoint=="viewContribution">
            <ul><li class="selected">view Contribution</li></ul>
            </#if>
          </li>
            </ul>

          </li>


         </li>

         </li>
      </ul>
  </li>
</ul>
</div>

<div class="nav-box">
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

<#if !Root.isEmbeddedMode()>
</div>

<div class="nav-box">
<h4>Browse documentation</h4>
<ul><li <#if navPoint=="documentation"> class="selected"</#if> >
<A href="${Root.path}/${distId}/doc"> FAQ and How to </A>
</li></ul>
</div>
</#if>

</#if>