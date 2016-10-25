<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
<div class="infoMessage">
 ${Context.getMessage('label.pkgNotFound.message')} '${pkgId?xml}'.
</div>
</@block>
</@extends>
