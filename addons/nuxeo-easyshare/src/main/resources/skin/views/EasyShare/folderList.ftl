<@extends src="base.ftl">

<@block name="content">

<div>
  <header>
  <!--
    <button><i class="icon-download"></i>Download</button>
  -->
    <h2>${docFolder.title}</h2>
    <detail>Shared by <a title="email address" href="mailto:${docFolder.easysharefolder.contactEmail}">
    ${docFolder.easysharefolder.contactEmail}</a></detail>
  </header>
  <content>
    <div class="comment">
      <i class="icon-user"></i>
      <blockquote>${docFolder.easysharefolder.shareComment}</blockquote>
    </div>
    <div class="shared-items">
    <#list docList as doc>
    
      <a class="item" title="document name" href="${docFolder.id}/${doc.id}/${This.getFileName(doc)}">
        <span class="document">
          <i class="icon-file"></i>${doc.title} - ${This.getFileName(doc)}
        </span>
        <i class="icon-download"></i>
      </a>
    </#list>
    <#if !docList>
      <div class="empty"><i class="icon-unhappy"></i>There is no files in this folder.</div>
    </#if>
  </content>
</div>

</@block>
</@extends>
