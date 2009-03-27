<#assign comments = This.comments />
<div class="commentspageBlock">
<h4>${Context.getMessage("label.page.comments.title")}</h4>
<#if (This.userWithCommentPermission==true)> 
<div class="addCommentLink">
  <a class="addComment" onclick="showCommentForm();">${Context.getMessage("label.page.comments.add")}</a>
</div>
</#if> 
 
<script type="text/javascript">
  function showCommentForm(){
    var e = document.getElementById("webComment");
    if(e.style.display == 'block')
      e.style.display = 'none';
    else
      e.style.display = 'block';
  } 
</script>

<form id="comment" action="${This.path}/@comments" method="POST" accept-charset="utf-8">  

  <div id="webComment" class="addWebComment" style="display: none">
    <textarea name="text" rows="4" cols="40" class="commentText">Your comment</textarea>
    <br/>
    <input type="submit" name="post_com" value="Add" id="post_com" class="button">
  </div>

  <div class="lastCommentsBlock">
    <#list comments as com>
    <div class="commentBlock">
      <div class="commentInfos">${com['webcomment:creationDate']} by ${com['webcomment:author']}</div>
      <div class="commentContent">${com['webcomment:text']}</div>
      <#if (This.moderator==true)>
        <a href="${This.path}/@comments/delete?property=${com.ref}">Delete</a>
      </#if> 
    </div>
    </#list>
  </div>

</form>
</div>
