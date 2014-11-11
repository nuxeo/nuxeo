<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 <div class="successfulDownloadBox">
        <h3> Removal of ${pkgId} completed </h3>

    <br/>

    <a href="${Root.path}/packages/${source}" class="button installButton"> Finish </a>
 </div>

</@block>
</@extends>