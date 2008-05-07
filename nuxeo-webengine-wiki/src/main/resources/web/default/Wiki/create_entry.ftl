<#assign name="${context.getFirstUnresolvedSegment()}">
<@extends src="/default/Wiki/base.ftl">
<@block name="content">
<h2>Create Wiki entry</h2>

<form method="POST" action="${this.docURL}/${name}@@create" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${name}" value="Title" /></h1>
<!--
Document Type:
<select name="doctype">
  <option>Wiki</option>
  <option selected="true">WikiPage</option>
</select>
-->

<textarea name="wp:content" cols="75" rows="30" class="entryEdit"></textarea>


<input type="hidden" name="doctype" value="WikiPage" id="doctype">
<!--p>
Description: <input type="text" name="dc:description" value=""/>
</p-->
<p class="buttonsGadget">
<input type="submit" class="button"/>
</p>
</form>

</@block>
</@extends>
