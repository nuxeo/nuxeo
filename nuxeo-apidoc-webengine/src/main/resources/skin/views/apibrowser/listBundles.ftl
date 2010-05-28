<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm bundleIds?size 'Bundle'/>

<#assign showDesc=false>
<#if Context.request.getParameter("showDesc")??>
  <#assign showDesc=true>
</#if>

<table width="100% class="tabs" id="tabbox">
  <tr>
    <td width="50%" >
     <div class="tabs
     <#if !showDesc>
       tabSelected
     </#if>
     "> <A href="${Root.path}/${distId}/listBundles">Introspection view</A></div>
    </td>
    <td width="50%" >
     <div class="tabs
     <#if showDesc>
       tabSelected
     </#if>
     "> <A href="${Root.path}/${distId}/listBundles?showDesc=true"> Documentation view</A></div>
    </td>
  </tr>
</table>

<#if Context.request.getParameter("showDesc")??>
   <#assign descriptions=This.getDescriptions("NXBundle")/>
</#if>

<#list bundleIds as bundleId>

  <A href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</A>
  <#if Context.request.getParameter("showDesc")??>
     <#assign desc=descriptions[bundleId]/>
    <@inlineEdit bundleId desc/>
  </#if>

  <br/>

</#list>

</@block>

</@extends>