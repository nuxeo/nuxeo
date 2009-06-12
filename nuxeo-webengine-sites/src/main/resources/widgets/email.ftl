<div>
    <table>
      <tr>
         <td>
          <a href="${siteCreatorEmail} ">${Context.getMessage("title.site.creator")}</a>
        </td>
      </tr>
    </table>
 </div>
 <form action="@rss/rssOnPage" method="get">
      <input type="image"  alt="" src="${skinPath}/images/feed.png"  value="submit"/>${Context.getMessage("label.rss.pages")}
      <input type="hidden" name="docId" value="${Document.id}" />
 </form>
 
  <form action="@rss/rssOnComments" method="get">
      <input type="image" alt="" src="${skinPath}/images/feed.png"  value="submit"/>${Context.getMessage("label.rss.comments")}
      <input type="hidden" name="docId" value="${Document.id}" />
 </form>

 