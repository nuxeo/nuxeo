<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<div class="explorer-home">
<div class="fullspace intro">
  <p class="main">
  Explore all services, extensions, contributions, operations of the Nuxeo Platform to customize it.
  </p>
  <p class="second">
  Nuxeo Platform provides Java Services declared inside components. Components are declared via XLM files. Services are configurable by an extension system. Thanks to the Nuxeo Platform modularity, declare your service and its extensions in a given component and contribute to this extension in an other component that might come with your customisation.
  </p>
  <#if Root.showCurrentDistribution()>
  <p class="second">
  Browse the running platform or a platform that has been snapshotted and saved into local Document Repository. Snapshotted platform are stored as documents and therefore can be searchable.
  </p>
  </#if>
</div>

<h2>Available Platforms</h2>

<#assign rtSnap=Root.runtimeDistribution/>
<#assign snapList=Root.listPersistedDistributions()/>

<div class="">

  <ul class="timeline">
  <#if Root.showCurrentDistribution()>
    <li>
      <time class="time" datetime="2013-04-10 18:30">
        <span class="date">${rtSnap.creationDate?date}</span>
        <span class="sticker current">Running Platform</span>
      </time>
      <div class="timepoint"></div>
      <div class="timebox">
        <div class="flex-1">
          <div class="box-title">
            <a href="${Root.path}/current/listBundles">
              <span class="number">${rtSnap.name}</span>
              <span class="detail">
                ${rtSnap.version}
              </span>
            </a>
          </div>
        </div>
        <div class="flex-2">
          <#if !Root.isSiteMode()>
            <div>
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
                  <td class="nowrap">Name</td>
                  <td><input type="text" name="name" value="${rtSnap.name}"/> </td>
                </tr>
                <tr>
                  <td class="nowrap">Version</td>
                  <td>${rtSnap.version} </td>
                </tr>
                </table>
                <i>Existing snapshot with the same name and version will be updated</i><br/>
                <input type="submit" value="Save"/>
              </form>
              </div>
              <div style="display:none" id="extendedSave">
              <form  method="POST" action="${Root.path}/saveExtended">
                <table>
                <tr>
                  <td class="nowrap">Name</td>
                  <td><input type="text" name="name" value="${rtSnap.name}"/> </td>
                </tr>
                <tr>
                  <td class="nowrap">Bundle prefixes</td>
                  <td><textarea rows="4" cols="30" name="bundles"></textarea></td>
                </tr>
                <tr>
                  <td class="nowrap">Packages prefixes</td>
                  <td><textarea rows="4" cols="30" name="packages"></textarea></td>
                </tr>
                </table>
              <input type="submit" value="Save"/>
              </form>
              </div>
            </#if>
            </div>
          </#if>
          <a class="button primary" href="${Root.path}/current/listBundles"> Explore </a>
        </div>
      </div>
    </li>
  </#if>

  <#list snapList as distrib>
    <li>
      <time class="time" datetime="2013-04-10 18:30">
        <span class="date">${distrib.creationDate?date}</span>
        <#if distrib.latestFT >
          <span class="sticker current">Latest FT</span>
        <#elseif distrib.latestLTS >
          <span class="sticker current">Latest LTS</span>
        <#else>
          &nbsp;
        </#if>
      </time>
      <div class="timepoint"></div>
      <div class="timebox">
        <div class="flex-1">
          <div class="box-title">
            <a href="${Root.path}/current/listBundles">
              <span class="number">${distrib.name}</span>
              <span class="detail">
                ${distrib.version}
              </span>
            </a>
          </div>
        </div>
        <div class="flex-2">
          <#if !Root.isSiteMode()>
          <a href="${Root.path}/download/${distrib.key}">ZIP Export</a>
          </#if>
          <a class="button primary" href="${Root.path}/${distrib.key}/listBundles">Explore</a>
      </div>
    </li>
  </#list>


  </ul>

</div>


<#if Root.isEditor()>
  <div class="fullspace">
    <h2>Add your distribution</h2>
    <form class="box" method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data" >
      <p>Upload your distribution that has been exported as a zip</p>
      <input type="file" name="archive">
      <input type="submit" value="Upload">
    </form>
  </div>
</#if>
<#if !Root.isSiteMode()>
  <div class="fullspace">
    <h2>Documentation</h2>
    <p>
      Documentation items are associated to Nuxeo Platform artifacts. The documentation contains currently ${Root.documentationInfo}. <a href="${Root.path}/downloadDoc">Export all documentation as a zip.</a>
    </p>
    <#if Root.isEditor()>
      <form method="POST" action="${Root.path}/uploadDoc" enctype="multipart/form-data" class="box">
        <p>Upload a documentation pack (zip)</p>
        <input type="file" name="archive">
        <input type="submit" value="Upload doc pack">
      </form>
    </#if>
  </div>
</#if>

</div>

</@block>

</@extends>
