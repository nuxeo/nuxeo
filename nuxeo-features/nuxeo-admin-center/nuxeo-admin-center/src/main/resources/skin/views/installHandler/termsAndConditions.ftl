<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">

   <h3> ${pkg.title} (${pkg.id}) </h3>

   <p>You need to accept the Terms and Condition before continuing the installation.</p>

   <div class="termsAndConditionsBlock" style="white-space:pre-line">
   ${content}
   </div>

    <div class="alignCenter">
      <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a> &nbsp;
      <a href="${Root.path}/install/start/${pkg.id}/?source=${source}&tacAccepted=true" class="installButton"> Accept </a>
   </div>

  </div>

</@block>
</@extends>