  <!-- markit up -->

<script type="text/javascript" src="/nuxeo/site/files/resources/script/markitup/jquery.markitup.pack.js"></script>
<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/sets/wiki/style.css" />

<!-- end markitup -->

<script>


  myWikiSettings = {
      nameSpace:          "wiki", // Useful to prevent multi-instances CSS conflict
      previewParserPath:  "/nuxeo/site/markitup_preview",
      previewParserVar: 'content',
      previewAutorefresh: true,
      previewInWindow: 'width=800, height=600, resizable=yes, scrollbars=yes',
      onShiftEnter:       {keepDefault:false, replaceWith:'\n\n'},
      markupSet:  [
          {name:'Heading 1', key:'1', openWith:'== ', closeWith:' ==', placeHolder:'Your title here...' },
          {name:'Heading 2', key:'2', openWith:'=== ', closeWith:' ===', placeHolder:'Your title here...' },
          {name:'Heading 3', key:'3', openWith:'==== ', closeWith:' ====', placeHolder:'Your title here...' },
          {name:'Heading 4', key:'4', openWith:'===== ', closeWith:' =====', placeHolder:'Your title here...' },
          {name:'Heading 5', key:'5', openWith:'====== ', closeWith:' ======', placeHolder:'Your title here...' },
          {separator:'---------------' },        
          {name:'Bold', key:'B', openWith:"'''", closeWith:"'''"}, 
          {name:'Italic', key:'I', openWith:"''", closeWith:"''"}, 
          {name:'Stroke through', key:'S', openWith:'<s>', closeWith:'</s>'}, 
          {separator:'---------------' },
          {name:'Bulleted list', openWith:'(!(* |!|*)!)'}, 
          {name:'Numeric list', openWith:'(!(# |!|#)!)'}, 
          {separator:'---------------' },
          {name:'Image', key:"T", replaceWith:'[[Image:[![Url:!:http://]!]|[![name]!]]]'}, 
          {name:'Link', key:"L", openWith:"[[![Link]!] ", closeWith:']', placeHolder:'Your text to link here...' },
          {name:'Url', openWith:"[[![Url:!:http://]!] ", closeWith:']', placeHolder:'Your text to link here...' },
          {separator:'---------------' },
          {name:'Quotes', openWith:'(!(> |!|>)!)'},
          {name:'Code', openWith:'{{{', closeWith:'}}}'}, 
          {separator:'---------------' },
          {name:'Preview', key: 'P', call:'preview', className:'preview'}
      ]
  }

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
