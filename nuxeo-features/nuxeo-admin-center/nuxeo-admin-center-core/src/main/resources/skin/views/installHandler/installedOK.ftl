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
   <h3>${Context.getMessage('label.installedOk.title.start')} ${pkg.title} (${pkg.id}) ${Context.getMessage('label.installedOk.title.end')}</h3>

    <#if installTask.isRestartRequired()>
     <div>
         ${Context.getMessage('label.installedOk.info.needrestart')}
         <br/>
         <form method="GET" action="${Root.path}/restartView">
	         ${Context.getMessage('label.installedOk.info.needclick')}
	         <input type="submit" value="Restart"/>
         </form>.
     </div>
    </#if>

    <br/>
    <#if source=="installer">
      <a href="javascript:closePopup()" class="button installButton">${Context.getMessage('label.installedOk.end')}</a>
    <#else>
      <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.installedOk.end')}</a>
    </#if>
  </div>
</@block>
</@extends>