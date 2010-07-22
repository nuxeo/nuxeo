<@extends src="baseProbe.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

    
    <h3>For server ${serverInstanceId}: </h3>
    the administrative status is: <p>${administrativeStatus} </p> 

<form name="lockServer" method="POST" 
  action="${This.path}/lock" accept-charset="utf-8">
  	    <input type="submit" class="button" value="Lock server" />&nbsp;
</form>
&nbsp;
<form name="unlockServer" method="POST" 
  action="${This.path}/unlock" accept-charset="utf-8">
  	    <input type="submit" class="button" value="Unlock server" />&nbsp;
</form>  
  


</@block>

</@extends>