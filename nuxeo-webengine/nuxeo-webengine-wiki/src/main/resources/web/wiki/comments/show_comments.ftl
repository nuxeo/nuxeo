<#assign comments = script("comments/getComments.groovy") />

  <h1>Comments</h1>
  
  <div class="commentsList">
  
    <ol id="comments">
      <#list comments as com>

      <#if com_index % 2 = 0>
        <#assign row = "even"/>
      <#else>
        <#assign row = "odd"/>
      </#if>

        <li class="comment ${row}" id="${com.ref}">
          <div class="byline">${com['comment:creationDate']} by ${com['comment:author']}</div>
          <p><quote>${com['comment:text']}</quote></p>
        </li>
      </#list>
    </ol>

  </div>
<#if Session.hasPermission(Document.ref, "Write")>
  <div class="addComment">
    <ul>
      <li>
        <form id="comment" action="${This.urlPath}@@add_comment" method="post" accept-charset="utf-8">
          <input type="text" name="author" value="Your name" id="author" class="commentAuthor">
          <br/>
          <textarea name="text" rows="4" cols="40" class="commentText">Your comment</textarea>
          <br/>
          <input type="submit" name="post_com" value="Add" id="post_com" class="commentAdd">
       </form>
     </li>
   </ul>
</div>
</#if>
