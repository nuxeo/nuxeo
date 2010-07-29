<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> Your current Nuxeo EP distribution is ${Root.currentDistribution.key} </h1>

<p>
 You can use this screen to browse your distribution.
</p>

<p>
 <table>
 <tr> <td> Number of bundles </td> <td> ${stats.bundles} </td></tr>
 <tr> <td> Number of java components </td> <td> ${stats.jComponents} </td></tr>
 <tr> <td> Number of Xml components </td> <td> ${stats.xComponents} </td></tr>
 <tr> <td> Number of services </td> <td> ${stats.services} </td></tr>
 <tr> <td> Number of Extension Points </td> <td> ${stats.xps} </td></tr>
 <tr> <td> Number of Contributions </td> <td> ${stats.contribs} </td></tr>
 </table>
</p>

<br/>
<br/>

</@block>

</@extends>