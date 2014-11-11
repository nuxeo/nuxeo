<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
   <h1> Installation of ${pkg.title} (${pkg.id}) is not possible </h1>

    <div class="installErrorTitle">
         Some dependencies can not be resolved.<br/>
         Installation process can not continue.
    </div>
    <br/>
    <br/>
    <ul class="installErrors">
       ${resolution.toString()}
    </ul>
    <br/>
    <br/>
    <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
  </div>

</@block>
</@extends>