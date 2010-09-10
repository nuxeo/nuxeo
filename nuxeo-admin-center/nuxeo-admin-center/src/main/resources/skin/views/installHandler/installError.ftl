<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
    <p>Installation failed : ${e.message}.</p>
    <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
  </div>
</@block>
</@extends>