<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">
<script>
	setTimeout(function(){window.location.replace("${redirectUrl}")},5000);
</script>
  
<div class="info">
    ${Context.getMessage('label.registerForm.welcome')}
    <#if isShibbo>
        ${Context.getMessage('label.registerForm.shibbo.redirect')}
        <#else>
        ${Context.getMessage('label.registerForm.redirect')}
    </#if>
</div>

</@block>
</@extends>