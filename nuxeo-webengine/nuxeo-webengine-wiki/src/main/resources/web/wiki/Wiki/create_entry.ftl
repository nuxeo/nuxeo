<#assign name="${Context.getFirstUnresolvedSegment()}">
<@extends src="Wiki/base.ftl">
<@block name="content">
<!-- markit up -->
<script src="/nuxeo/site/files/resources/script/markitup/jquery.markitup.pack.js"></script>
<script src="/nuxeo/site/files/resources/script/markitup/sets/wiki/set.js"></script>
<!-- end markitup -->

<script>

function launchEditor() {
  $('#content').markItUp(myWikiSettings)
}

$('#content').ready(function(){
  setTimeout(launchEditor, 10)
  })
</script>

<h2>Create Wiki Page</h2>
<form method="POST" action="${This.urlPath}/${name}@@create" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${name}" value="Title" /></h1>

<p>
<textarea name="wp:content" id="content" cols="75" rows="30"></textarea>
</p>

<input type="hidden" name="doctype" value="WikiPage" id="doctype">

<p class="buttonsGadget">
  <input type="submit"/>
</p>
</form>
</@block>
</@extends>
