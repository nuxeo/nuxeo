<table width="90%">
<tr>
  <td> ${docItem.content?html}"</td>
  <td>
    <table>
      <tr>
        <td> Approved by Nuxeo : </td>
        <td>
        <#if docItem.approved>
            true
        </#if>
        <#if !docItem.approved>
            true
        </#if>
        </td>
    </tr>
    <tr>
        <td> Applicable versions : </td>
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
