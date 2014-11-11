<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">

   <h3> ${pkg.title} (${pkg.id}) </h3>

   <p><a href="https://www.nuxeo.com/en/services/nuxeo-admin-center-tc" target="_blank">You need to accept the Terms and Conditions before continuing the installation.</a></p>

   <div class="termsAndConditionsBlock" style="white-space:pre-line">
   ${content}
   </div>

    <div class="alignCenter">
      <a href="${Root.path}/packages/${source}" class="button installButton"> Cancel </a> &nbsp;
      <a href="${Root.path}/install/start/${pkg.id}/?source=${source}&tacAccepted=true" class="button installButton"> Accept </a>
   </div>

  </div>

</@block>
</@extends>
