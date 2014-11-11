<div>
<#list entries as entry>
<div class="post">
  <h2><a class="title" href="${entry.url}"
      rel="bookmark">${entry.title?html}</a></h2>

  <div class="info">
    <span class="date">${entry.published?string("MMMM dd, yyyy")}</span>
    <span class="author"><span class="by">By</span>
      <!-- FIXME -->
      <a class="url fn" href="${entry.parentBlog.url}"
          title="View all posts by oleg">${entry.parentBlog.title?html}</a></span>

    <div class="act">
      <span class="comments"><a target="" href="${entry.url}/#respond"
          title="Comment on ${entry.title?html}">Comments</a></span>

      <div class="fixed"></div>
    </div>
    <div class="fixed"></div>
  </div>
  <div class="content">
    ${entry.content}

    <p class="under"><span class="categories">
      <#list entry.tags as tag>
        <a href="${blog.url}/tag/${tag}/">${tag}</a>,
      </#list>
    </span></p>

    <div class="fixed"></div>
  </div>
</div>
</#list>
</div>
