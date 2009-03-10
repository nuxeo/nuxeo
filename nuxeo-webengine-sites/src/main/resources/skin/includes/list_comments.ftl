<#assign comments = This.comments />
 
 <#if (This.userWithCommentPermission==true)> 
   <input type="button" value="addComment" onclick="showCommentForm();"/>
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

<div class="commentsList">
  <ol id="comments">
    <#list comments as com>
      <#if com_index % 2 = 0>
        <#assign row = "even"/>
      <#else>
        <#assign row = "odd"/>
      </#if>
      <li class="comment ${row}" id="${com.ref}">
        <div class="byline">${com['webcomment:creationDate']} by ${com['webcomment:author']}</div>
        <p><quote>${com['webcomment:text']}</quote></p>
        <#if (This.moderator==true)>
          <a href="${This.path}/@comments/delete?property=${com.ref}">Delete</a>
         </#if> 
          <br/>
      </li>
      </li>
    </#list>
  </ol>
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

