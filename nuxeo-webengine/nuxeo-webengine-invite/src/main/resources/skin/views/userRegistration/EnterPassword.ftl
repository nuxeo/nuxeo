<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">
<form action="${data['ValidationUrl']}" method="post" enctype="application/x-www-form-urlencoded" name="submitNewPassword">
	<input type="hidden" id="RequestId" value="${data['RequestId']}" name="RequestId"/>
	<input type="hidden" id="ConfigurationName" value="${data['ConfigurationName']}" name="ConfigurationName"/>
	<#if err??>
	  <div class="errorMessage">
	    ${Context.getMessage("label.connect.trial.form.errvalidation")}
	    ${err}
	  </div>
	</#if>
	<#if info??>
	  <div class="infoMessage">
	    ${info}
	  </div>
	</#if>
	<div class="info">${Context.getMessage('label.registerForm.title')}</div>
	<div>
	  <input placeholder="${Context.getMessage('label.registerForm.password')}" type="password" id="Password" value="${data['Password']}" name="Password" class="login_input" isRequired="true" autofocus required/>
	  <i class="icon-key"></i>
	</div>
	<div>
    <input placeholder="${Context.getMessage('label.registerForm.passwordConfirmation')}" type="password" id="PasswordConfirmation" value="${data['PasswordConfirmation']}" name="PasswordConfirmation" class="login_input" isRequired="true" required/>
    <i class="icon-key"></i>
	</div>
	<div>
	  <input type="submit" name="submit" value="${Context.getMessage('label.registerForm.submit')}" />
	</div>
</form>

</@block>
</@extends>