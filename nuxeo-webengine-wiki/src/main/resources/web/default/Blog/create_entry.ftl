<@extends src="/default/Blog/base.ftl">
<@block name="content">

<h2>Create Wiki Page : ${This.title} </h2>
<form method="POST" action="${This.docURL}@@create">

<p>
<label for="dc:title">Title</label>
<input type="text" name="dc:title" value="${name}" size=75/>
</p>

<p>
<label for="bp:content">Content</label>
<textarea name="bp:content" cols="75" rows="30"></textarea>
</p>

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