<h1>${This.title}</h1>
<h2>Versions</h2>

<form id="version_list" action="${This.urlPath}@@compare_versions" method="get" accept-charset="utf-8">
<table class="version_list">
    <tr>
        <td>From</td>
        <td>To</td>
        <td>Version</td>
        <td>Editor</td>
        <td>Date</td>
        <td>Comment</td>
    </tr>
    <#list Document.versions as rev>
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
        <td><input type="radio" name="r1" value="${Document.ref}"/></td>
        <td><input type="radio" name="r2" value="${Document.ref}"/></td>
        <td>current</td>
        <td>${Document.creator}</td>
        <td>${Document.modified?datetime}</td>
        <td></td>
    </tr>
    
</table>
<p class="buttonsGadget">
  <input type="submit" name="Compare" value="Compare" id="compare_versions" class="button">
</p>
</form>