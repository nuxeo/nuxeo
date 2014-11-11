<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<ul>
    <li>Get this server <a href="${This.path}/Probes">operational statuses</a></a></li>
    <li>Get this server <a href="${This.path}/AdministrativeStatus">administrative status</a></li>
    <li>Check this server <a href="${This.path}/Probes/availability">availability</a></li>
</ul>
</@block>

</@extends>