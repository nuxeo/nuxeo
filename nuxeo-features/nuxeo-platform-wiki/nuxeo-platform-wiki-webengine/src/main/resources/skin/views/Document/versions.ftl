<@extends src="base.ftl">
<@block name="content">

<#import "common/util.ftl" as base/>
<h2>Versions</h2>

<form id="version_list" action="${This.path}/@views/compare_versions" method="get" accept-charset="utf-8">
<table class="itemListing history">
  <thead>
    <tr>
        <th>Version</th>
        <th>Author</th>
        <th>Date</th>
        <th>Comment</th>
    </tr>
  </thead>
  <tbody>
    <#list Document.versions as rev>
    <tr>
        <td><a href="${This.path}/@versions/${rev.versionLabel}">${rev.versionLabel}</a></td>
        <td>${rev.author}</td>
        <td>${rev.modified?datetime}</td>
        <td>&nbsp;</td>
    </tr>
    </#list>
  </tbody>
</table>
</form>

</@block>
</@extends>
