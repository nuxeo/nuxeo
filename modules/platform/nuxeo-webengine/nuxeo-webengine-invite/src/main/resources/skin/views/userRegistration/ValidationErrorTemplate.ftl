<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">

<div class="info">
<p>
<#if exceptionMsg??>
  Your invitation cannot be validated : "${exceptionMsg?html}".
</#if>
<#if error??>
  An error occured during your invitation processs.
</#if>
</p>
</div>

</@block>
</@extends>
