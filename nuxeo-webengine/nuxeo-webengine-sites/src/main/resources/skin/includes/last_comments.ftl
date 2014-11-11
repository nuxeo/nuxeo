<#macro lastComments>
<div class="lastCommentsBlock">
  <h4>${Context.getMessage("title.last.published.comments")}</h4>
  <#list comments as com>
  <div class="commentBlock">
  <div class="commentInfos">${com.day}  ${com.month} by ${com.author} sur ${com.pageTitle}</div>
  <div class="commentContent">${com.content}</div>
  </div>
  </#list>
</div>
</#macro>
