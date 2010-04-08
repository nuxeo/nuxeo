<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h2>Nuxeo EP Distributions</h2>

<#assign rtSnap=Root.runtimeDistribution/>
<#assign snapMap=Root.persistedDistributions/>

<table width="90%">
<tr>
<th> Distribution name  </th>
<th> Version  </th>
<th> Creation date  </th>
<th> &nbsp;  </th>
<th> &nbsp;  </th>
<th> &nbsp;  </th>
</tr>

<tr>
<td>${rtSnap.name}</td>
<td>${rtSnap.version}</td>
<td>${rtSnap.creationDate?datetime}</td>
<td>Current deployed distribution (live) </td>
<td>
  <A href="${Root.path}/${rtSnap.key}/"> Browse </A>
</td>
<td>
  <form method="POST" action="${Root.path}/save">
  <input type="submit" value="Save">
  </form>
</td>
</tr>

<#assign names=snapMap?keys/>
<#list names as name>
<tr>
  <#assign distrib=snapMap[name]/>
  <td>${distrib.name}</td>
  <td>${distrib.version}</td>
  <td>${distrib.creationDate?datetime}</td>
  <td>&nbsp; </td>
  <td>
    <A href="${Root.path}/${distrib.key}/"> Browse </A>
  </td>
  <td>
    <A href="${Root.path}/download/${distrib.key}">Export</A> as zip.<br/><br/>
  </td>
</tr>
</#list>

</table>

<br/>
<form method="POST" action="${Root.path}/uploadDistrib" enctype="multipart/form-data" >
  <input type="file" name="archive">
  <input type="submit" value="Upload additionnal distribution">
</form>


<br/>

<h2>Documentation</h2>

Documentation contains currently ${Root.documentationInfo}
<br/>

<A href="${Root.path}/downloadDoc">Export</A> all documentation as a zip.<br/><br/> <br/>
<A>Import</A> documentation as zip.<br/>

<br/>
<form method="POST" action="${Root.path}/uploadDoc" enctype="multipart/form-data" >
  <input type="file" name="archive">
  <input type="submit" value="Upload documentation pack">
</form>


</@block>

</@extends>