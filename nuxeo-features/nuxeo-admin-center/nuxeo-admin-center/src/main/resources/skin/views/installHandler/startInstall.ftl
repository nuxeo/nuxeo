<@extends src="base.ftl">

<@block name="header_scripts">
<script>
function closePopup() {
 self.close();
}
</script>
</@block>

<@block name="body">

  <div class="genericBox">

   <h1> Installation of ${pkg.title} (${pkg.id}) </h1>

   <#if status.hasWarnings()>
      <div class="installWarningsTitle">
         Some warnings where found when checking the package
      </div>
      <ul class="installWarnings">
        <#list status.getWarnings() as warning>
          <li> ${warning} </li>
        </#list>
      </ul>
   </#if>

   <#if installTask.isRestartRequired()>
     <div>
         After installation, you will need to restart your server.
     </div>
   </#if>

   <br/>

   <#if needWizard>
      Click the start link to start the installation wizard. <br/><br/><br/>
      <A href="${Root.path}/install/form/${pkg.id}/0?source=${source}" class="installButton"> Start </a>
   </#if>

   <#if !needWizard>
      Click the start link to start the installation process. <br/><br/><br/>
      <a href="${Root.path}/install/run/${pkg.id}?source=${source}" class="installButton"> Start </a>
   </#if>

   &nbsp;
   <#if source=="installer">
     <a href="javascript:closePopup()" class="installButton"> Cancel </a>
   <#else>
     <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
   </#if>
  </div>

</@block>
</@extends>