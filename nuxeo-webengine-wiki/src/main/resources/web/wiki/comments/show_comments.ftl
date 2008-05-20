<#assign comments = script("comments/getComments.groovy") />

<h2>Comments</h2>
  
<ol id="comments">
<#list comments as com>

<#if com_index % 2 = 0>
    <#assign row = "even"/>
  <#else>
    <#assign row = "odd"/>
</#if>

<li class="comment ${row}" id="${com.ref}">
  <quote>${com['comment:text']}</quote>
  <div class="byline">By ${com['comment:author']} @ ${com['comment:creationDate']}</div>
</li>
</#list>
</ol>


<form id="comment" action="${This.urlPath}@@add_comment" method="post" accept-charset="utf-8">
  <label for="author">Author</label>
  <input type="text" name="author" value="" id="author">
  <br/>
  <label for="author">Comment's Text</label>
  <textarea name="text" rows="4" cols="40"></textarea>
  <br/>
  <input type="submit" name="post_com" value="Comment!" id="post_com">
</form>

<form action="${This.urlPath}@@add_comment" >
</form>
