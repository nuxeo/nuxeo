<!--#import "common/util.ftl" as base/-->
<h1>${Document.title}</h1>

<form id="version_list" action="${This.path}/@views/compare_versions" method="get" accept-charset="utf-8">
  <table class="itemListing history">
    <thead>
      <tr>
        <th>From</th>
        <th>To</th>
        <th>Version</th>
        <th>Author</th>
        <th>Date</th>
        <th>Comment</th>
      </tr>
    </thead>
    <tbody>  
      <#list Document.versions as rev>
      <tr>
        <td><input type="radio" name="r1" value="${rev.ref}"/></td>
        <td><input type="radio" name="r2" value="${rev.ref}"/></td>
        <td><a href="${This.path}/@versions/${rev.versionLabel}">${rev.versionLabel}</a></td>
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
        <td>&nbsp;</td>
      </tr>
    </tbody>
  </table>
  <p class="buttonsGadget">
    <input type="submit" name="Compare" value="Compare" id="compare_versions" class="button">
  </p>
</form>