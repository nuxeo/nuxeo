<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> Nuxeo Platform Explorer </h1>

<p>
This application allows you to explore Nuxeo Enterprise Platform.
</p>

<p>
You can explore the current live Nuxeo distribution (i.e. the one that runs this server)
or browse a distribution that has been snapshotted and saved into local Document Repository.
</p>

<p>
Keep in mind that only snapshotted distributions (i.e non-live) are stored as documents and
therefore they are the only one to be searchable.
</p>

<h2>Nuxeo EP Distributions</h2>

<p>
Here are the currently available distributions:
</p>

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
<td style="color:green">Current deployed distribution (live) </td>
<td>
  <A href="${Root.path}/current/"> Browse </A>
</td>
<td>
<#if Root.isEditor()>
  <form method="POST" action="${Root.path}/save">
  <input type="submit" value="Save">
  </form>
</#if>
</td>
</tr>

<#assign names=snapMap?keys/>
<#list names as name>
<tr><td colspan="6">&nbsp;</td></tr>
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
    <A href="${Root.path}/download/${distrib.key}">Export</A> as zip.
  </td>
</tr>
</#list>

</table>

<br/>
<#if Root.isEditor()>
<p> You can use this form to upload a distribution that has been exported as a zip.
</p>
<form method="POST" action="${Root.path}/uploadDistrib" enctype="multipart/form-data" >
  <input type="file" name="archive">
  <input type="submit" value="Upload">
</form>
</#if>


<br/>

<h2>Documentation</h2>

<p>
Documentation items are associated to the Nuxeo Platform artifacts.<br/>
Documentation packs can be downloaded or uploaded here.
</p>

<p>
Documentation contains currently ${Root.documentationInfo}
</p>

<a href="${Root.path}/downloadDoc">Export</a> all documentation as a zip.<br/><br/> <br/>

<br/>
<#if Root.isEditor()>
You can use this form to upload a Documentation pack (zip).<br/>
<form method="POST" action="${Root.path}/uploadDoc" enctype="multipart/form-data" >
  <input type="file" name="archive">
  <input type="submit" value="Upload doc pack">
</form>
</#if>

</@block>

</@extends>