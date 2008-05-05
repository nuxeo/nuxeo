<@extends src="/default/WikiPage/view.ftl">
<@block name="content">

<h1>Versions for ${this.title}</h1>

<form action="${this.docURL}@@compare_versions" method="get" accept-charset="utf-8">
<table class="version_list">
    <tr>
        <td>From</td>
        <td>To</td>
        <td>Version</td>
        <td>Editor</td>
        <td>Date</td>
        <td>Comment</td>
    </tr>
    <#list this.versions as rev>
    <tr>
        <td><input type="radio" name="r1" value="${rev.ref}"/></td>
        <td><input type="radio" name="r2" value="${rev.ref}"/></td>
        <td>${rev.versionLabel}</td>
        <td>${rev.author}</td>
        <td>${rev.modified?datetime}</td>
        <td>&nbsp;</td>
    </tr>
    </#list>
    <tr>
        <td><input type="radio" name="r1" value="${this.ref}"/></td>
        <td><input type="radio" name="r2" value="${this.ref}"/></td>
        <td>current</td>
        <td>${this.author}</td>
        <td>${this.modified?datetime}</td>
        <td></td>
    </tr>
    
</table>
<input type="submit" name="Compare" value="Compare" id="compare_versions">
</form>
</@block>
</@extends>
