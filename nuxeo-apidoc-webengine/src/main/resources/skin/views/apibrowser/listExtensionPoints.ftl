<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm eps?size 'ExtensionPoint'/>

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
     "> <A href="${Root.path}/${distId}/listExtensionPoints">Introspection view</A></div>
    </td>
    <td width="50%" >
     <div class="tabs
     <#if showDesc>
       tabSelected
     </#if>
     "> <A href="${Root.path}/${distId}/listExtensionPoints?showDesc=true"> Documentation view</A></div>
    </td>
  </tr>
</table>

<#if Context.request.getParameter("showDesc")??>
   <#assign descriptions=This.getDescriptions("NXExtensionPoint")/>
</#if>

<#list eps as ep>

  <A href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.label}</A>
  <#if Context.request.getParameter("showDesc")??>
     <#assign cid=ep.id/>
     <#assign desc=descriptions[cid]/>
    <@inlineEdit cid desc/>
  </#if>

  <br/>

</#list>

</@block>

</@extends>