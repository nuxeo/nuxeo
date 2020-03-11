<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">

   <h3> ${pkg.title} (${pkg.id}) </h3>

   <p><a href="http://www.nuxeo.com/services/nuxeo-admin-center-terms-conditions/" target="_blank">${Context.getMessage('label.termsAndConditions.message.accept')}</a></p>

   <div class="termsAndConditionsBlock" style="white-space:pre-line">
   ${content}
   </div>

    <div class="alignCenter">
      <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.termsAndConditions.buttons.cancel')}</a> &nbsp;
      <a href="${Root.path}/install/start/${pkg.id}/?source=${source?xml}&tacAccepted=true" class="button installButton">${Context.getMessage('label.termsAndConditions.buttons.accept')}</a>
   </div>

  </div>

</@block>
</@extends>
