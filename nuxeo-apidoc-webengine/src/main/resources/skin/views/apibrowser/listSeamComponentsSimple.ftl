<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#if searchFilter??>
  <h1> Deployed Seam Components listing (with filter '${searchFilter}') </h1>
</#if>
<#if !searchFilter>
  <h1> Deployed Seam Components listing </h1>
</#if>

<table>
<tr>
	<th>Seam Component Name</th>
	<th>Class name</th>
	<th>Scope</th>
</tr>
<#list seamComponents as component>
<#assign rowCss = (component_index % 2 == 0)?string("row_even","row_odd")/>
 <tr class="${rowCss}">
     <td style="padding:2px"><b>${component.name}</b> </td>
     <td>${component.className} </td>
     <td>${component.scope}</td>
  </tr>
 <tr >
     <td> &nbsp; </td>
     <td colspan="2" style="background-color:#F8F8FF">
     <#list component.interfaceNames as iface>
     	<br/>${iface}
     	<#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(iface)}"/>
     	<#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${iface?replace('.','/')}.html"/>
	    &nbsp;&nbsp;&nbsp;  <span class="resourceToggle"> JavaDoc </span>
	    <div class="hiddenResource">
	      <A href="${javaDocUrl}" target="NxJavaDoc">Open in a new window</A>
	      <iframe src="${javaDocUrl}" width="98%" height="300px" border="0"></iframe>
	    </div>
     </#list>
     <br/>
     </td>
  </tr>

</#list>
</table>

</@block>

</@extends>