<#macro lastComments>
<table border="1">
  <tr>${Context.getMessage("title.last.published.comments")}</tr>
	<tr>
    <#list comments as com>
      <li class="comment ${row}">
        <div class="byline">${com.day}  ${com.month} by ${com.author}</div>
        <p><img src="${skinPath}/image/user.gif" alt="" /> sur ${com.pageTitle} </p>
        <p>${com.content}</p>
      </li>
    </#list>
    </tr>
</table>
</#macro>
