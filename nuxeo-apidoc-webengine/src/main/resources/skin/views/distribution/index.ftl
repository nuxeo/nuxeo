<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1>Welcome to Nuxeo Platform Explorer</h1>

<h2>Nuxeo EP Distributions</h2>

<p>
Here are the currently available distributions:
</p>

<#assign rtSnap=Root.runtimeDistribution/>
<#assign snapList=Root.listPersistedDistributions()/>

<table class="tablesorter distributions">
<tr>
<th> Name</th>
<th> Version</th>
<th> Creation date</th>
<th></th>
<th></th>
<#if !Root.isSiteMode() >
<th></th>
</#if>
</tr>

  <#if Root.showCurrentDistribution()>
  <tr>
  <td>${rtSnap.name}</td>
  <td>${rtSnap.version}</td>
  <td>${rtSnap.creationDate?datetime}</td>
  <td><span class="sticker current">Current deployed distribution (live)</span></td>

  <td>
    <p><a class="button" href="${Root.path}/current/listBundles"> Explore </a></p>
  </td>
<#if !Root.isSiteMode()>
  <td>
  <#if Root.isEditor()>
    <div id="saveBtn">
    <form method="POST" action="${Root.path}/save">
    <input type="button" value="Save" onclick="$('#stdSave').css('display','block');$('#saveBtn').css('display','none')">
    <input type="button" value="Save Partial Snapshot" onclick="$('#extendedSave').css('display','block');$('#saveBtn').css('display','none')">
    </form>
    </div>
    <div style="display:none" id="stdSave">
    <form method="POST" action="${Root.path}/save">
      <table>
      <tr>
        <td>Name : </td>
        <td><input type="text" name="name" value="${rtSnap.name}"/> </td>
      </tr>
      <tr>
        <td>Version : </td>
        <td>${rtSnap.version} </td>
      </tr>
      </table>
      <i>( Existing snapshot with the same name and version will be updated )</i><br/>
      <input type="submit" value="Save"/>
    </form>
    </div>
    <div style="display:none" id="extendedSave">
    <form  method="POST" action="${Root.path}/saveExtended">
      <table>
      <tr>
        <td>Name : </td>
        <td><input type="text" name="name" value="${rtSnap.name}"/> </td>
      </tr>
      <tr>
        <td>Bundle prefixes : </td>
        <td><textarea rows="4" cols="30" name="bundles"></textarea></td>
      </tr>
      <tr>
        <td>Packages prefixes : </td>
        <td><textarea rows="4" cols="30" name="packages"></textarea></td>
      </tr>
      </table>
    <input type="submit" value="Save"/>
    </form>
    </div>

  </#if>
  </td>
</#if>
</tr>
</#if>

<#list snapList as distrib>
<tr>
  <td>${distrib.name}</td>
  <td>${distrib.version}</td>
  <td>${distrib.creationDate?datetime}</td>
  <td>
    <#if distrib.latestFT >
      <span class="sticker current">Latest FT</span>
    <#elseif distrib.latestLTS >
      <span class="sticker current">Latest LTS</span>
    <#else>
      &nbsp;
    </#if>
  </td>
  <td>
    <p class="explore"><a class="button" href="${Root.path}/${distrib.key}/listBundles"> Explore </a></p>
  </td>
<#if !Root.isSiteMode()>
  <td>
    <p class="explore export"><a class="button" href="${Root.path}/download/${distrib.key}">Export as zip.</a></p>
  </td>
</#if>
</tr>
</#list>

</table>

<br/>
<#if Root.isEditor()>
<p> You can use the form below to upload a distribution that has been exported as a zip:
</p>
<form method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data" >
  <input type="file" name="archive">
  <input type="submit" value="Upload">
</form>
</#if>

    </td>
  </tr>
  <tr>
    <td width="50%">
      <h2>What is Nuxeo Platform Explorer?</h2>
      <p>
        This application allows you to explore Nuxeo Enterprise Platform.
      </p>
      <p>
      <#if Root.showCurrentDistribution()>
      <p>
        You can explore the current live Nuxeo distribution (i.e. the one that runs this server) or browse a distribution that has been snapshotted and saved into local Document Repository.
      </p>
      <p>
        Keep in mind that only snapshotted distributions (i.e non-live) are stored as documents and therefore they are the only one to be searchable.
      </p>
      <#else>
      <p>You can browse a distribution that has been snapshotted and saved into local Document Repository.</p>
      </#if>
    </td>
    <td width="50%">
<#if !Root.isSiteMode()>
  <div>
    <h2>Documentation</h2>

    <p>
      Documentation items are associated to the Nuxeo Platform artifacts.<br/>
      Documentation packs can be downloaded or uploaded here.
    </p>
    <p>
      Documentation contains currently ${Root.documentationInfo}
    </p>

    <p><a href="${Root.path}/downloadDoc" class="button">Export all documentation as a zip.</a></p>
    <#if Root.isEditor()>
      <p>You can use the form below to upload a documentation pack (zip):</p>
      <form method="POST" action="${Root.path}/uploadDoc" enctype="multipart/form-data">
        <input type="file" name="archive">
        <input type="submit" value="Upload doc pack">
      </form>
    </#if>
  </div>
</#if>

</@block>

</@extends>
