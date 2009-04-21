<#assign publishedComments = This.publishedComments />
<#assign pendingComments = This.pendingComments /> 

<div class="commentspageBlock">
<#if (publishedComments?size != 0 || base.canWrite)>
  <h4>${Context.getMessage("label.page.comments.title")}</h4>
</#if>
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

<form id="comment" action="${This.path}/@webcomments" method="POST" accept-charset="utf-8">  

  <div id="webComment" class="addWebComment" style="display: none">
    <textarea name="text" rows="4" cols="40" class="commentText">Your comment</textarea>
    <br/>
    <input type="submit" name="post_com" value="Add" id="post_com" class="button">
  </div>

  <div class="lastCommentsBlock">
    <#list publishedComments as com>
    <div class="commentBlock">
      <div class="commentInfos">${com['comment:creationDate']} by ${com['comment:author']}</div>
      <div class="commentContent">${com['comment:text']}</div>
      	<#if (This.moderator==true)>
        	<a href="${This.path}/@webcomments/delete?property=${com.ref}">Delete</a>
      	</#if>
    </div>
    </#list>
  </div>

  <div class="lastCommentsBlock">
    <#if (This.moderator==true)>
    <#list pendingComments as com>
    <div class="commentBlock">
      <div class="commentInfos">${com['comment:creationDate']} by ${com['comment:author']}</div>
      <div class="commentContent">${com['comment:text']}</div>
        <a href="${This.path}/@webcomments/reject?property=${com.ref}">Reject</a>
          <br/>
          <a href="${This.path}/@webcomments/approve?property=${com.ref}">Approve</a>
    </div>
    </#list>
    </#if> 
  </div>
  
  <div>${This.commentMessage}</div>

</form>
</div>


