<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

  <div class="genericBox">
    <h1><b> ${form.title} </b></h1>
    <br />
    ${form.description}
    <br /><br />

    <h2> Step ${step} / ${steps} </h2>

     <form method="POST" action="${Root.path}/install/form/${pkg.id}/${step-1}?source=${source?xml}">

       <table>
       <#list form.getFields() as field>
          <tr>
            <td class="labelColumn"> ${field.label} </td>
            <td> <input type="text" name="${field.name}" required="${field.required}" value="${field.value}"/> </td>
          </tr>
       </#list>

       </table>

       <input class="button installButton" type="submit" value="Validate">
       &nbsp; <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.showInstallForm.buttons.cancel')}</a>

     </form>
   </div>

</@block>
</@extends>
