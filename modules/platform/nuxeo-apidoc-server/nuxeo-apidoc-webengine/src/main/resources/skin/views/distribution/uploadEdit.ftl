<@extends src="base.ftl">

<@block name="left" />

<@block name="right">

  <div>
    The distribution zip you have uploaded contains <b>${snapObject.bundleIds?size} bundles</b>.
  </div>
  <div>
    You can edit some of the properties of the distribution before you validate the upload:<br/>
  </div>

  <form method="POST" action="${Root.path}/uploadDistribTmpValid">
    <table>
      <tr>
        <td>Name</td>
        <td>
          <input type="text" name="name" value="${tmpSnap.nxdistribution.name}" width="100%" />
        </td>
      </tr>
      <tr>
        <td>Version</td>
        <td>
          <input type="text" name="version" value="${tmpSnap.nxdistribution.version}" width="100%" />
        </td>
      </tr>
      <tr>
        <td>Path Segment</td>
        <td>
          <input type="text" name="pathSegment" value="${tmpSnap.name}" width="100%" />
        </td>
      </tr>
      <tr>
        <td>Title</td>
        <td>
          <input type="text" name="title" value="${tmpSnap.title}" width="100%" />
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="hidden" name="source" value="${source}" />
          <input type="submit" value="Import bundles" id="doImport" onclick="$.fn.clickButton(this)" />
        </td>
      </tr>
    </table>
  </form>

</@block>

</@extends>
