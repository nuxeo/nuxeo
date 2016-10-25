<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

  <div class="errorDownloadBox">
     <h3>Uninstall failed : ${e.message}.</h3>

    <br/>
    <a href="${Root.path}/packages/${source?xml}" class="button installButton"> Cancel </a>
  </div>

</@block>
</@extends>