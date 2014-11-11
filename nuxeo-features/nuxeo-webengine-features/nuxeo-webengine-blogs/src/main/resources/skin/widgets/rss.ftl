 <div class="rssFeedBloc">

 <form action="@rss/rssOnPage" method="get">
      <input class="rssButton" type="submit" value="${Context.getMessage("label.blog.rss.pages")}">
      <input type="hidden" name="docId" value="${This.getIdForRss()}" />
 </form>
 
 <form action="@rss/rssOnComments" method="get">
      <input class="rssButton" type="submit" value="${Context.getMessage("label.blog.rss.comments")}">
      <input type="hidden" name="docId" value="${Document.id}" />
 </form>
 
 </div>