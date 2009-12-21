<#assign sections = Session.getDocument(docRef("/default-domain/sections")) />

<form name="publish" method="GET" action="${Document.urlPath}/@publish">
<#list sections.children as section>
  <div>
  <input type="checkbox" name="sections" value="${section.path}">${section.title}
  </div>
</#list>
<p>&nbsp;</p>
<input type="submit" value="Publish">
</form>
