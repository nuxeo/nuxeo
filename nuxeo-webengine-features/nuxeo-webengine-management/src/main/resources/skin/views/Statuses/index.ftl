<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<ul>
    <li>Check this server <a href="${This.path}/availability">availability</a></li>
    <li>Get this server <a href="${This.path}/probes">probe statuses</a></a></li>
    <li>Get this server <a href="${This.path}/admin">administrative status</a></li>
</ul>
</@block>

</@extends>