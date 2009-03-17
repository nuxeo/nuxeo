<#assign publishedComments = This.publishedComments />
<#assign pendingComments = This.pendingComments /> 
 
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

<div class="publishedCommentsList">
  <ol id="publishedComments">
    <#list publishedComments as com>
      <#if com_index % 2 = 0>
        <#assign row = "even"/>
      <#else>
        <#assign row = "odd"/>
      </#if>
      <li class="comment ${row}" id="${com.ref}">
        <div class="byline">${com['webcomment:creationDate']} by ${com['webcomment:author']}</div>
        <quote>${com['webcomment:text']}</quote>
        <#if (This.moderator==true)>
          <a href="${This.path}/@comments/delete?property=${com.ref}">Delete</a>
         </#if> 
          <br/>
      </li>
      </li>
    </#list>
  </ol>
</div>

<div class="pendingCommentsList">
  <#if (This.moderator==true)>
  <ol id="pendingComments">
    <#list pendingComments as com>
      <#if com_index % 2 = 0>
        <#assign row = "even"/>
      <#else>
        <#assign row = "odd"/>
      </#if>
      <li class="comment ${row}" id="${com.ref}">
        <div class="byline">${com['webcomment:creationDate']} by ${com['webcomment:author']}</div>
        <p><quote>${com['webcomment:text']}</quote></p>
        
          <a href="${This.path}/@comments/delete?property=${com.ref}">Delete</a>
          <br/>
            <a href="${This.path}/@comments/reject?property=${com.ref}">Reject</a>
          <br/>
          <a href="${This.path}/@comments/approve?property=${com.ref}">Approve</a>
      </li>
      </li>
    </#list>
     </#if> 
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

