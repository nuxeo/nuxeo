<#assign name="${Context.getFirstUnresolvedSegment()}">
<@extends src="Blog/base.ftl">
<@block name="content">

<h2>New blog post</h2>
<form method="POST" action="${This.urlPath}/${name}@@create" accept-charset="utf-8">
<h1>
<input type="text" name="dc:title" value="${name}" size=75/>
</h1>

<textarea name="bp:content" cols="75" rows="30" class="entryEdit"></textarea>

<p class="entryEditOptions">
    Allow Trackbacks:
    <input type="radio" name="trackback" value="yes" checked /> Yes
    <input type="radio" name="trackback" value="yes" /> No
</p>

<p class="entryEditOptions">
    Allow Comments:
    <input type="radio" name="comment" value="yes" checked /> Yes
    <input type="radio" name="comment" value="yes" /> No
</p>


<input type="hidden" name="doctype" value="BlogPost" id="doctype">
<p class="buttonsGadget">
<input type="submit" class="button"/> 
</p>
</form>

</@block>
</@extends>
