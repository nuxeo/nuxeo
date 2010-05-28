<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm cIds?size 'Contribution'/>

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
     "> <A href="${Root.path}/${distId}/listContributions">Introspection view</A></div>
    </td>
    <td width="50%" >
     <div class="tabs
     <#if showDesc>
       tabSelected
     </#if>
     "> <A href="${Root.path}/${distId}/listContributions?showDesc=true"> Documentation view</A></div>
    </td>
  </tr>
</table>

<#if Context.request.getParameter("showDesc")??>
   <#assign descriptions=This.getDescriptions("NXContribution")/>
</#if>

<#list cIds as cId>

  <A href="${Root.path}/${distId}/viewContribution/${cId}">${cId}</A>
    <#if Context.request.getParameter("showDesc")??>
     <#assign desc=descriptions[cId]/>
    <@inlineEdit cId desc/>
  </#if>
  <br/>

</#list>

</@block>

</@extends>