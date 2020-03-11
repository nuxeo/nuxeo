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
   <h3> Uninstallation of ${pkg.title} (${pkg.id}) will be done at next restart.</h3>
    <br/>
    <div>
     To complete the uninstallation of the package, please restart your server.
    </div>
    <br/>
    <#if source=="installer">
      <a href="javascript:closePopup()" class="button installButton"> Finish </a>
    <#else>
      <a href="${Root.path}/packages/${source?xml}" class="button installButton"> Finish </a>
    </#if>
  </div>
</@block>
</@extends>