<@extends src="base.ftl">

  <@block name="content">

  <div>

    <#include "includes/header.ftl">

    <content>

      <#if docFolder.easysharefolder.shareComment?length &gt; 0 >
        <div class="comment">
          <i class="icon-user"></i>
          <blockquote>${docFolder.easysharefolder.shareComment}</blockquote>
        </div>
      </#if>

      <div class="shared-items">
        <#list docList as doc>
          <#assign filename=This.getFileName(doc)>
          <a class="item" title="${filename}" href="${docFolder.id}/${doc.id}/${filename}">
        <span class="document">
          <i class="icon-file"></i>${doc.title}<#if filename != doc.title> - ${filename}</#if>
        </span>
            <i class="icon-download"></i>
          </a>
        </#list>

        <#if !docList>
          <div class="empty"><i class="icon-unhappy"></i>${Context.getMessage("easyshare.label.nofiles")}</div>
        </#if>

    </content>

  </div>

  </@block>
</@extends>
