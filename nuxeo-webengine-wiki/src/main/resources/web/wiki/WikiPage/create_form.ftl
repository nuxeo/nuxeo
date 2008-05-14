<#assign name="${Context.getFirstUnresolvedSegment()}">
<@extends src="Wiki/base.ftl">
<@block name="content">
<h2>Create Wiki Page : ${This.title} </h2>
<form method="POST" action="${This.urlPath}/${name}@@create" accept-charset="utf-8">
<!--
Document Type:
<select name="doctype">
  <option>Wiki</option>
  <option selected="true">WikiPage</option>
</select>
-->
<p>
<textarea name="wp:content" cols="75" rows="30"></textarea>
</p>
<p>
Title: <input type="text" name="dc:title" value="${name}"/>
</p>

<input type="hidden" name="doctype" value="WikiPage" id="doctype">
<!--p>
Description: <input type="text" name="dc:description" value=""/>
</p-->
<input type="submit"/>
</form>
</@block>
</@extends>
