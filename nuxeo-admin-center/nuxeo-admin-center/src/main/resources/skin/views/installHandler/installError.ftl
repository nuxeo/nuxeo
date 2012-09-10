<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
    <h3>Installation failed : ${e.message}.</h3>
    <a href="${Root.path}/packages/${source}" class="button installButton"> Cancel </a>
  </div>
</@block>
</@extends>