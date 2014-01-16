<@extends src="base.ftl">

<@block name="content">

<div style="margin: 10px 10px 10px 10px">


<h2>${docFolder.name}</h2>
<p>${docFolder.easysharefolder.shareComment}</p>
<p>These files were shared with you by <a href="mailto:${docFolder.easysharefolder.contactEmail}">${docFolder.easysharefolder.contactEmail}</a> </p>

<br>

<#list docList as doc>
      <li>${doc.name} - <a href="${docFolder.id}/${doc.id}/${doc.file.filename}">${doc.file.filename}</a>
</#list>

</@block>
</@extends>
