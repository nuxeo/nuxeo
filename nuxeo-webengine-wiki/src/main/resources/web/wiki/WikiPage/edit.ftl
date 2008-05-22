  <!-- markit up -->

<script src="/nuxeo/site/files/resources/script/markitup/jquery.markitup.pack.js"></script>
<script src="/nuxeo/site/files/resources/script/markitup/sets/wiki/set.js"></script>


<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/sets/wiki/style.css" />

<!-- end markitup -->

<script>
  $(function() {  
   $('#content').markItUp(myWikiSettings);
  })
</script>

<form method="POST" action="${This.urlPath}@@update" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${Document.dublincore.title}"/></h1>
  <textarea name="wp:content" cols="75" rows="40" id="content" class="entryEdit">${Document.wikiPage.content}</textarea>
  <p class="entryEditOptions">
    Version increment:
    <input type="radio" name="versioning" value="major" checked> Major
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value="minor"/> Minor
  </p>
  <p class="buttonsGadget">
    <input type="submit" class="button"/>
  </p>
</form>
</body>
