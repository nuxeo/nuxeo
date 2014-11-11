<@extends src="base.ftl">
<@block name="header"><h1><a href="${basePath}">Create Document</a></h1></@block>
<@block name="content">

<#if This.isRoot()>
<h2>${Document.title}</h2>
<#else>
<h2><a href="${This.previous.path}"><img src="${skinPath}/image/up_nav.gif" alt="Up" border="0"/></a> ${Document.title}</h2>
</#if>

<div id="mainContentBox">

<form action="${This.path}" method="POST">
<table class="formFill"> 
    <tbody> 
        <tr>
            <td>Name:</td>
            <td><input type="text" name="name" value="" size="40"></td>
            <input type="hidden" name="doctype" value="Wiki" />
        </tr>

        <tr>
            <td>Title:</td>
            <td><input type="text" name="dc:title" value="" size="40"/></td>
        </tr>
        <tr>
            <td colspan="2">Description</td>
        </tr>
        <tr>
            <td colspan="2"><textarea name="dc:description" cols="54"></textarea></td>
        </tr>
        <tr>
          <td colspan="2" align="right"><input type="submit" value="Save"/></td>
        </tr>
    </tbody>
</table>  
</form>

</div>

</@block>
</@extends>
