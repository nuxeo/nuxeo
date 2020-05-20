<@extends src="base.ftl">

  <@block name="stylesheets">
  </@block>

  <@block name="header_scripts">
  </@block>

  <@block name="right">
  <h1>Distributions</h1>

    <#assign rtSnap=Root.runtimeDistribution/>
    <#assign snapList=Root.listPersistedDistributions()/>

  <table class="tablesorter distributions">
    <tr>
      <th>Name</th>
      <th>Version</th>
      <th>Creation date</th>
      <th>Release date</th>
      <th>Flags</th>
      <th>Actions</th>
    </tr>

    <tr>
      <td><a class="button" href="${Root.path}/current/">${rtSnap.name}</a></td>
      <td>${rtSnap.version}</td>
      <td>${rtSnap.creationDate?datetime}</td>
      <td>-</td>
      <td><span class="sticker current">Current deployed distribution (live)</span></td>
      <td>
        <div id="saveBtn">
          <form method="POST" action="${Root.path}/save">
            <input type="button" value="Save" id="save"
              onclick="$('#stdSave').css('display','block');$('#saveBtn').css('display','none')">
            <input type="button" value="Save Partial Snapshot" id="savePartial"
              onclick="$('#extendedSave').css('display','block');$('#saveBtn').css('display','none')">
          </form>
          <form method="GET" action="${Root.path}/json">
            <input type="submit" value="Export as Json" />
          </form>
        </div>
        <div style="display:none" id="stdSave">
          <form method="POST" action="${Root.path}/save">
            <table>
              <tr>
                <td class="nowrap">Name</td>
                <td><input type="text" name="name" value="${rtSnap.name}"/></td>
              </tr>
              <tr>
                <td class="nowrap">Release Date</td>
                <td><input type="date" name="released" placeholder="yyyy-MM-dd" /></td>
              </tr>
              <tr>
                <td class="nowrap">Version</td>
                <td><span name="version">${rtSnap.version}</span></td>
              </tr>
            </table>
            <i>Existing snapshot with the same name and version will be updated.</i><br/>
            <input type="submit" value="Save" id="doSave" class="button" />
          </form>
        </div>
        <div style="display:none" id="extendedSave">
          <form method="POST" action="${Root.path}/saveExtended">
            <table>
              <tr>
                <td class="nowrap">Name</td>
                <td><input type="text" name="name" value="${rtSnap.name}"/></td>
              </tr>
              <tr>
                <td class="nowrap">Release Date</td>
                <td><input type="date" name="released" /></td>
              </tr>
              <tr>
                <td class="nowrap">Bundle prefixes</td>
                <td><textarea rows="4" cols="30" name="bundles"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Packages prefixes</td>
                <td><textarea rows="4" cols="30" name="packages"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Version</td>
                <td><span name="version">${rtSnap.version}</span></td>
              </tr>
            </table>
            <input type="submit" value="Save" id="doSaveExtended" class="button" />
          </form>
        </div>

      </td>
    </tr>

    <#list snapList as distrib>
      <tr>
        <td><a class="button" href="${Root.path}/${distrib.key}/">${distrib.name}</a></td>
        <td>${distrib.version}</td>
        <td>${distrib.creationDate?datetime}</td>
        <td>${distrib.releaseDate?datetime}</td>
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
          <p><a class="button" href="${Root.path}/download/${distrib.key}">Export as zip</a></p>
        </td>
      </tr>
    </#list>

  </table>

  <h1>Upload Distribution</h1>
  <div class="box">
    <p>You can use the form below to upload a distribution that has been exported as a zip:</p>
    <form method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data">
      <input type="file" name="archive" id="archive">
      <input type="submit" value="Upload" id="upload">
    </form>
  </div>

  </@block>
</@extends>
