<@extends src="base.ftl">

<@block name="title">Update Distribution</@block>

<@block name="left" />

<@block name="right">

  <h1>Update Distribution '${properties["nxdistribution:key"]}'</h1>

  <form method="POST" action="${Root.path}/doUpdate">
    <table class="dataInput">
      <tr>
        <td class="label required">Title</td>
        <td>
          <input type="text" name="dc:title" value="${properties['dc:title']}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Name</td>
        <td>
          <input type="text" name="nxdistribution:name" value="${properties['nxdistribution:name']}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Version</td>
        <td>
          <input type="text" name="nxdistribution:version" value="${properties['nxdistribution:version']}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Key</td>
        <td>
          <input type="text" name="nxdistribution:key" value="${properties['nxdistribution:key']}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label">Released</td>
        <td>
          <input type="date" name="nxdistribution:released" value="${properties['nxdistribution:released']}" />
        </td>
      </tr>
      <tr>
        <td class="label">Aliases</td>
        <td>
          <textarea rows="5" name="nxdistribution:aliases" width="100%">${properties['nxdistribution:aliases']}</textarea>
        </td>
      </tr>
      <tr>
        <td class="label">Latest LTS</td>
        <td>
          <input type="checkbox" name="nxdistribution:latestLTS" <#if properties['nxdistribution:latestLTS'] == 'true'>checked</#if> />
        </td>
      </tr>
      <tr>
        <td class="label">Latest FT</td>
        <td>
          <input type="checkbox" name="nxdistribution:latestFT" <#if properties['nxdistribution:latestFT'] == 'true'>checked</#if> />
        </td>
      </tr>
      <tr>
        <td class="label">Hidden</td>
        <td>
          <input type="checkbox" name="nxdistribution:hide" <#if properties['nxdistribution:hide'] == 'true'>checked</#if> />
        </td>
      </tr>
      <tr>
        <td class="label">Update Comment</td>
        <td>
          <textarea rows="4" name="comment" width="100%"></textarea>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="hidden" name="docId" value="${distribDoc.id}" />
          <input type="hidden" name="distribId" value="${distribId}" />
          <input type="submit" class="button primary" value="Update" id="doUpdate" onclick="$.fn.clickButton(this)" />
          <a class="button" href="${Root.path}/_admin/">Cancel</a>
        </td>
      </tr>
    </table>
  </form>

</@block>

</@extends>
