<@extends src="base.ftl">

<@block name="left" />

<@block name="right">

  <h1>Distribution Snapshot persistence failed</h1>
  <div id="details" class="message error">
    Details: ${message}
  </div>
  <div>
    <#assign view=This.getRedirectViewPostUpload(source) />
    <a href="${Root.path}/${view}" class="button">Retry</a>
  </div>

</@block>

</@extends>
