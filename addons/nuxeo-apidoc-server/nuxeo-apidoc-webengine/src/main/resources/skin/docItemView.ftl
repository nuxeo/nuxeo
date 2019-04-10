<table width="100%">
<tr>
  <td width="70%">
  <#if docItem.renderingType=='html'>
    ${docItem.content}
  </#if>
  <#if docItem.renderingType=='wiki'>
    <@wiki>${docItem.content}</@wiki>
  </#if>
  </td>
  <td>
    <table>
      <tr>
        <td> Approved by Nuxeo: </td>
        <td> ${docItem.approved?string("yes", "no")} </td>
    </tr>
    <tr>
        <td> Applicable versions: </td>
        <td>
        <#list docItem.applicableVersion as version>
           ${version} <br/>
        </#list>
        </td>
    </tr>
    </table>
  </td>
 </tr>
</table>
