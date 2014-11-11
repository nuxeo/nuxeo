<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 Uninstall failed : ${e.message}.

 <br/>
 <A href="${Root.path}/packages/${source}" class="installButton"> Cancel </A>
</@block>
</@extends>