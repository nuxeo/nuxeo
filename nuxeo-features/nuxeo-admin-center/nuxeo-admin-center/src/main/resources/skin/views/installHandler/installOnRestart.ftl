<@extends src="base.ftl">

<@block name="header_scripts">
<script>
function closePopup() {
 self.close();
}
</script>
</@block>

<@block name="body">
  <div class="errorDownloadBox">
   <h1> Installation of ${pkg.title} (${pkg.id}) can not be completed now.</h1>
    <br/>
    <div>
     Because your current Operating System locks the resources loaded by the JVM, we can not complete the installation now.
    </div>
    <div>
     Installation will be completed on next server restart.
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