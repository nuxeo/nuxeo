<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h2>Nuxeo EP Distributions</h2>

<h3>Current deployed distribution </h3> 

<A href="${Root.path}/${Root.runtimeDistributionName}/">${Root.runtimeDistribution.key}</A> 

<form method="POST" action="${Root.path}/save">
<input type="submit" value="persist">
</form>

<br/>  

<h3>Persisted distributions </h3>
<ul> 
<#list Root.persistedDistributions as distrib>

<li>
 <A href="${Root.path}/${distrib}/">${distrib}</A><br/>
</li>
</#list>
</ul>

</@block>

</@extends>