<@extends src="./base.ftl">

<@block name="title">
 ${Context.getMessage('label.registerForm.title')}
</@block>

<@block name="content">
  
<div class="registrationForm">
<form action="${This.path}/validate" method="post" enctype="application/x-www-form-urlencoded" name="submitNewPassword">
	<input type="hidden" id="RequestId" value="${key}" name="RequestId"/>
	<table>
		<tr><td colspan="2">${Context.getMessage('label.registerForm.title')}</td></tr>
	 	<tr>
	    	<td class="login_label">
				<span class="required">${Context.getMessage('label.registerForm.password')}</span>
	        </td>
	        <td>
				<input type="password" id="Password" value="${data['Password']}" name="Password" class="login_input" isRequired="true"/>
	        </td>
		</tr>
	    <tr>
	        <td class="login_label">
				<span class="required">${Context.getMessage('label.registerForm.passwordConfirmation')}</span>
	        </td>
	        <td>
				<input type="password" id="PasswordConfirmation" value="${data['PasswordConfirmation']}" name="PasswordConfirmation" class="login_input" isRequired="true"/>
	        </td>
		</tr>
	
		<tr>
	        <td></td>
	        <td>
	        	<input class="login_button" type="submit" name="submit" value="${Context.getMessage('label.registerForm.submit')}" />
	        </td>
	    </tr>
	    
	    <#if err??>
	    <tr>
	      <td colspan="2">
	        <div class="errorMessage">
	          ${Context.getMessage("label.connect.trial.form.errvalidation")}
	          ${err}
	        </div>
	      </td>
	    </tr>
	  </#if>
	
	  <#if info??>
	    <tr>
	      <td colspan="2">
	        <div class="infoMessage">
	          ${info}
	        </div>
	      </td>
	    </tr>
	  </#if> 
	
	</table>
</form>

</div>

</@block>
</@extends>