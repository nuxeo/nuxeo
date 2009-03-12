<#macro lastComments>
<table>
  <tr><th>${Context.getMessage("title.last.published.comments")}</th></tr>
  <#list comments as com>
  <tr>
    <td>
      <table>
        <tr>
          <td><img src="${skinPath}/image/user.gif" alt="" /> ${com.day}  ${com.month} by ${com.author} sur ${com.pageTitle}</td>
        </tr>
        <tr>
          <td>${com.content}</td>
        </tr>  
      </table>
    </td>
  </tr>
  </#list>
</table>
</#macro>
