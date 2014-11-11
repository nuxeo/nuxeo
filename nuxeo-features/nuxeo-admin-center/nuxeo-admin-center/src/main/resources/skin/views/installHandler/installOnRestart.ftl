<@extends src="base.ftl">

<@block name="header_scripts">
<script>
function closePopup() {
 self.close();
}
</script>
</@block>

<@block name="body">
  <div class="successfulDownloadBox">
   <h1> Installation of ${pkg.title} (${pkg.id}) will be done at next restart.</h1>
    <br/>
    <div>
     To complete the installation of the package, please restart your server.
    </div>
    <div>
     <string>Please read carefully the description of the package, in case any manual update is needed.</strong>
    </div>
    <br/>
    <#if source=="installer">
      <a href="javascript:closePopup()" class="installButton"> Finish </a>
    <#else>
      <a href="${Root.path}/packages/${source}" class="installButton"> Finish </a>
    </#if>
  </div>
</@block>
</@extends>