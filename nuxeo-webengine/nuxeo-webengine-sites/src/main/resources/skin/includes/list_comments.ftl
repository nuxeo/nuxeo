<#assign comments = This.comments />
 
 <#if (This.userWithCommentPermission==true)> 
   <a onclick="showCommentForm();"><img src="${skinPath}/images/action_add.gif" alt="${Context.getMessage("label.page.comments.add")}">${Context.getMessage("label.page.comments.add")}</a>
 </#if> 
 
 <script type="text/javascript">
 function showCommentForm(){
 	var com=document.getElementById('webComment');
 	if(com){
 		if(com.style.visibility=='hidden'){
 			com.style.visibility='visible';
 		}else{
 			com.style.visibility=='hidden';
 		}
 	}
 }
 </script>

<form id="comment" action="${This.path}/@comments" method="POST" accept-charset="utf-8">  
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

<div id="webComment" class="addWebComment" style="visibility:hidden">
    <ul>
      <li>
          <textarea name="text" rows="4" cols="40" class="commentText">Your comment</textarea>
          <br/>
          <input type="submit" name="post_com" value="Add" id="post_com" class="commentAdd">
     </li>
   </ul>
</div>
</form>

