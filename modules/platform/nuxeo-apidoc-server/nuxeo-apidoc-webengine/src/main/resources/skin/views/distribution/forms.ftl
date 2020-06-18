<@extends src="base.ftl">

<@block name="right">

  <h1>Distributions</h1>

  <table class="tablesorter distributions">
    <tr>
      <th>Name</th>
      <th>Version</th>
      <th>Creation date</th>
      <th>Release date</th>
      <th>Flags</th>
      <th>Actions</th>
    </tr>

    <#if Root.showRuntimeSnapshot()>
    <#assign rtSnap=Root.runtimeDistribution/>
    <tr>
      <td><a class="button currentDistrib" href="${Root.path}/current/">${rtSnap.name}</a></td>
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
            <input type="hidden" name="source" value="admin">
            <input type="submit" value="Save" id="doSave" class="button primary" onclick="$.fn.clickButton(this)" />
            <input type="button" value="Cancel" id="save"
              onclick="$('#stdSave').css('display','none');$('#saveBtn').css('display','block')">
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
                <td class="nowrap">JAVA Packages prefixes</td>
                <td><textarea rows="4" cols="30" name="javaPackages"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Nuxeo Packages prefixes</td>
                <td><textarea rows="4" cols="30" name="nxPackages"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Version</td>
                <td><span name="version">${rtSnap.version}</span></td>
              </tr>
            </table>
            <input type="hidden" name="source" value="admin">
            <input type="submit" value="Save" id="doSaveExtended" class="button primary" onclick="$.fn.clickButton(this)" />
            <input type="button" value="Cancel" id="save"
              onclick="$('#extendedSave').css('display','none');$('#saveBtn').css('display','block')">
          </form>
        </div>

      </td>
    </tr>
    </#if>

    <#list Root.listPersistedDistributions() as distrib>
      <tr>
        <td><a class="distrib button" href="${Root.path}/${distrib.key}/">${distrib.name}</a></td>
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
          <p>
            <a class="button" href="${Root.path}/download/${distrib.key}" onclick="$.fn.clickButton(this)">
              Export as zip
            </a>
          </p>
        </td>
      </tr>
    </#list>

  </table>

  <h1>Upload Distribution</h1>
  <div class="box">
    <p>You can use the form below to upload a distribution that has been exported as a zip:</p>
    <form method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data">
      <input type="file" name="archive" id="archive">
      <input type="hidden" name="source" value="admin">
      <input type="submit" value="Upload" id="upload" onclick="$.fn.clickButton(this)">
    </form>
  </div>

</@block>

</@extends>
