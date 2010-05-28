<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm services?size 'Service'/>

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
     "> <A href="${Root.path}/${distId}/listServices">Introspection view</A></div>
    </td>
    <td width="50%" >
     <div class="tabs
     <#if showDesc>
       tabSelected
     </#if>
     "> <A href="${Root.path}/${distId}/listServices?showDesc=true"> Documentation view</A></div>
    </td>
  </tr>
</table>

<#if Context.request.getParameter("showDesc")??>
   <#assign descriptions=This.getDescriptions("NXService")/>
</#if>

<#list services as service>

  <A href="${Root.path}/${distId}/viewService/${service.id}">${service.label}</A>
  <#if Context.request.getParameter("showDesc")??>
     <#assign cid=service.id/>
     <#assign desc=descriptions[cid]/>
    <@inlineEdit cid desc/>
  </#if>
  <br/>

</#list>

</@block>

</@extends>