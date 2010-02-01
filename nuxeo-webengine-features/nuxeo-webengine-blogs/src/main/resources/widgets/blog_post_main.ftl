<div class="postBloc">
  <div class="postHeading">
    <div class="postDate">${Document.dublincore.created}</div>
    <div class="postAuthor">Posted by ${Document.dublincore.creator}</div>
    <div style="clear:both;"></div>
  </div>
  <div class="postHeadtitle">
    <div class="postTitle">${Document.dublincore.title?xml}</div>
    <div class="postSubtitle">${Document.dublincore.description?xml}</div>
  </div>
  <div class="postContent">${Document.webpage.content}</div>
</div>
