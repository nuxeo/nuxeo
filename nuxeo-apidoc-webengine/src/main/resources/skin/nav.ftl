<#if Root.currentDistribution!=null>

<#assign navPoint=Root.getNavigationPoint()/>

<#if !Root.isEmbeddedMode()>

<A href="${Root.path}"> See all available distributions </A>

<h3> ${Root.currentDistribution.name} ${Root.currentDistribution.version}
<#if Root.currentDistribution.isLive()>
 (Live)
</#if>
</h3>

<h3>Nuxeo Runtime </h3>


<b> Browse by category </b>
<table border=0 style="padding:0px;margin:0px">
<tr>
    <td class="spacerTab"></td>
    <td colspan="4" <#if navPoint=="listOperations"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listOperations">
       Operations
      </a>
      <#if navPoint=="viewOperation">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Operation </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td colspan="4" <#if navPoint=="listSeamComponents"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listSeamComponents">
       Seam components
      </a>
      <#if navPoint=="viewSeamComponent">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Seam component </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td colspan="4" <#if navPoint=="listBundleGroups"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listBundleGroups">
       Bundle groups
      </a>
      <#if navPoint=="viewBundleGroup">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Bundle group </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td colspan="3" <#if navPoint=="listBundles"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listBundles">
       Bundles
      </a>
      <#if navPoint=="viewBundle">
       <div class="currentNavPoint"> &nbsp;&nbsp; > view Bundle </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td colspan="2" <#if navPoint=="listComponents"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listComponents">
       Components
      </a>
      <#if navPoint=="viewComponent">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Component </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td <#if navPoint=="listServices"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listServices">
       Services
      </a>
      <#if navPoint=="viewService">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Service </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td <#if navPoint=="listExtensionPoints"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listExtensionPoints">
       Extension points
      </a>
      <#if navPoint=="viewExtensionPoint">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Extension point </div>
      </#if>
    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td <#if navPoint=="listContributions"> class="currentNavPoint" </#if> >
      <a href="${Root.path}/${distId}/listContributions">
       Contributions
      </a>
      <#if navPoint=="viewContribution">
        <div class="currentNavPoint"> &nbsp;&nbsp; > view Contribution </div>
      </#if>
    </td>
</tr>
</table>

<br/>
<b> Browse by hierarchy </b>
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
<br/>
<h3>Browse documentation</h3>
<table border=0>
<tr>
      <td colspan="4" style="font-weight:bold"
    <#if navPoint=="documentation">
     class="currentNavPoint"
    </#if>

      >
      <A href="${Root.path}/${distId}/doc"> FAQ and How to </A>
      </td>
</tr>
</table>
</#if>

</#if>