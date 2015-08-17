<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">
<script>
	setTimeout(function(){window.location.replace("${logout}")},5000);
</script>
  
<div class="info">
    Welcome! You account is now created.
    <#if isShibbo>
        You are going to be redirected to access the platform through Shibboleth.
        <#else>
        You are going to be redirected to access the platform.
    </#if>
</div>

</@block>
</@extends>