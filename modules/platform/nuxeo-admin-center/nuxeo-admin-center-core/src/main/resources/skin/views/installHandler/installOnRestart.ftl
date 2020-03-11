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
   <h3>${Context.getMessage('label.installOnRestart.title.start')} ${pkg.title} (${pkg.id}) ${Context.getMessage('label.installOnRestart.title.end')}</h3>
    <br/>
    <div>
     ${Context.getMessage('label.installOnRestart.message.restart')}
    </div>
    <div>
     <strong>${Context.getMessage('label.installOnRestart.message.read')}</strong>
    </div>
    <br/>
    <#if source=="installer">
      <a href="javascript:closePopup()" class="button installButton">${Context.getMessage('label.installOnRestart.buttons.finish')}</a>
    <#else>
      <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.installOnRestart.buttons.finish')}</a>
    </#if>
  </div>
</@block>
</@extends>