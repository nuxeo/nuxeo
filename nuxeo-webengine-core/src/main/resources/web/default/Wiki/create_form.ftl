<#assign name="${Context.getFirstUnresolvedSegment()}">
<@extends src="/default/base.ftl">
<@block name="content">
<h2>Create Wiki Page : ${This.document.title} </h2>
<form method="POST" action="${This.urlPath}/${name}@@create">
Document Type:
<select name="doctype">
  <option>Wiki</option>
  <option selected="true">WikiPage</option>
</select>
<p>
<textarea name="wp:content" cols="80" rows="20"></textarea>
</p>
<p>
Title: <input type="text" name="dc:title" value="${name}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value=""/>
</p>
<input type="submit"/>
</form>
</@block>
</@extends>
