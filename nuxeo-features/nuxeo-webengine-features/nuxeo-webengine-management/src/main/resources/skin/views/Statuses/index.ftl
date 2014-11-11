<@extends src="base.ftl">

<@block name="content">
<h2>Statuses</h2>
This page gives you access to this server statuses. Follow the links provided in the toolbox.
</@block>

<@block name="toolbox">
<ul><h3>Toolbox</h3>
    <li>Check this server <a href="${This.path}/probes/availability">availability</a></li>
    <li>List <a href="${This.path}/probes">probe statuses</a></a></li>
    <li>Get this server <a href="${This.path}/admin">administrative status</a></li>
</ul>
</@block>

</@extends>