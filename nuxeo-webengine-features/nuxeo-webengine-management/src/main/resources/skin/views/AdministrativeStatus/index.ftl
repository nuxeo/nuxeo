<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">

    
    <h3>For server ${serverInstanceId} </h3>
    the administrative status is <emph>${administrativeStatus}</emph>

<p/>
<#if administrativeStatus == 'passive'>
<form method="POST" 
  action="${This.path}/activate" accept-charset="utf-8">
  	    <input type="submit" class="button" value="Activate" />
</form>
<#else>
<form method="POST" 
  action="${This.path}/passivate" accept-charset="utf-8">
  	    <input type="submit" class="button" value="Passivate" />
</form>  
</#if>

</@block>

</@extends>