<div class="AuthorBloc">
 <h4>${Context.getMessage("title.contact.creator")}</h4> 
 <a href="${siteCreatorEmail} ">${Context.getMessage("title.site.creator")}</a>
 </div>

 <div class="rssFeedBloc">
    <h4>${Context.getMessage("title.rss.feeds")}</h4> 
 
 <form action="@rss/rssOnPage" method="get">
      <input class="rssButton" type="submit" value="${Context.getMessage("label.rss.pages")}">
      <input type="hidden" name="docId" value="${Document.id}" />
 </form>
 
  <form action="@rss/rssOnComments" method="get">
      <input class="rssButton" type="submit" value="${Context.getMessage("label.rss.comments")}">
      <input type="hidden" name="docId" value="${Document.id}" />
 </form>
 
 </div>