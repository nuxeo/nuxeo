<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">

   <h1> ${pkg.title} (${pkg.id}) </h1>

   You need to accept the Terms and Condition before continuing the installation.

   <br/>

   <pre>
   ${content}
   </pre>

   <br/>

   <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a> &nbsp;
   <A href="${Root.path}/install/start/${pkg.id}/?source=${source}&tacAccepted=true" class="installButton"> Accepts </a>

  </div>

</@block>
</@extends>