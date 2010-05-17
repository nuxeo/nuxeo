<#if Root.currentDistribution!=null>

<#assign navPoint=Root.getNavigationPoint()/>

<h3>Browse distributions</h3>
<b> Browse by category </b>
<table border=0 style="padding:0px;margin:0px">
<tr>
      <td colspan="5" style="font-weight:bold">
      <A href="${Root.path}"> Distributions </A>
      </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td colspan="4"

    <#if navPoint=="listBundleGroups">
     class="currentNavPoint"
    </#if>
    >
      <A href="${Root.path}/${distId}/listBundleGroups">
       Artifacts groups
      </A>

    <#if navPoint=="viewBundleGroup">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view Bundle group
     </div>
    </#if>


    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td colspan="3"
    <#if navPoint=="listBundles">
     class="currentNavPoint"
    </#if>

    >
      <A href="${Root.path}/${distId}/listBundles">
       Bundles
      </A>

    <#if navPoint=="viewBundle">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view Bundle
     </div>
    </#if>

    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td colspan="2"
    <#if navPoint=="listComponents">
     class="currentNavPoint"
    </#if>

    >
      <A href="${Root.path}/${distId}/listComponents">
       Components
      </A>

    <#if navPoint=="viewComponent">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view Component
     </div>
    </#if>

    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td
    <#if navPoint=="listServices">
     class="currentNavPoint"
    </#if>

    >
      <A href="${Root.path}/${distId}/listServices">
       Services
      </A>

    <#if navPoint=="viewService">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view Service
     </div>
    </#if>

    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td
    <#if navPoint=="listExtensionPoints">
     class="currentNavPoint"
    </#if>
    >
      <A href="${Root.path}/${distId}/listExtensionPoints">
       ExtensionPoints
      </A>

     <#if navPoint=="viewExtensionPoint">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view ExtensionPoint
     </div>
    </#if>

    </td>
</tr>
<tr>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td class="spacerTab"></td>
    <td
    <#if navPoint=="listContributions">
     class="currentNavPoint"
    </#if>
    >
      <A href="${Root.path}/${distId}/listContributions">
       Contributions
      </A>

     <#if navPoint=="viewContribution">
     <div class="currentNavPoint">
     &nbsp;&nbsp; > view Contribution
     </div>
    </#if>

    </td>
</tr>
</table>
<br/>
<b> Browse by hierarchy </b>

<div id="treeControler"></div>

<#macro tree id url="${Root.path}/${distId}/tree" root="/">
  <script type="text/javascript">
  $(document).ready(function(){
    $("#${id}").treeview({
      url: "${url}",
      root: "${root}",
      animated: "fast",
      unique: true,
      control: "#treeControler",
      collapsed: true,
      persist: "cookie"
    });
  });
  </script>

  <ul id="${id}" class="filetree">
  </ul>

</#macro>
<@tree id="myTree" root="/"/>

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