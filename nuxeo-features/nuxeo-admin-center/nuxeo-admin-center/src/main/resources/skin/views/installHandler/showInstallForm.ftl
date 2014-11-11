<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

  <div class="genericBox">
    <h1> Installation of ${pkg.title} (${pkg.id}) :</h1>
   
    <h2> Step ${step} / {steps} </h2>
   
     <form method="POST" action="${Root.path}/install/form/${pkg.id}/${step-1}?source=${source}">
   
       <table>
       <#list form.getFields() as field>
          <tr>
            <td class="labelColumn"> ${field.label} </td>
            <td> <input type="text" name="${field.name}" value="${field.value}"/> </td>
          </tr>
       </#list>
   
       </table>
   
       <input type="submit"/>
       &nbsp; <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
   
     </form>
   </div>

</@block>
</@extends>