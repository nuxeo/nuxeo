<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">
<script>
	setTimeout(function(){window.location.replace("${logout}")},5000);
</script>
  
<div class="info">
Welcome! You account has been created.
You are now going to be redirected automatically to the logout page...
</div>

</@block>
</@extends>