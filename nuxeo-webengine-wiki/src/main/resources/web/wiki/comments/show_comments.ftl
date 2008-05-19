<#assign runcomment = Context.runScript("comments/getComments.groovy") />

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

