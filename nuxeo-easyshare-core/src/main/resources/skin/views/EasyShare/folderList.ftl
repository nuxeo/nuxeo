<@extends src="base.ftl">

  <@block name="content">

  <div>

    <#include "includes/header.ftl">

    <content>

      <#if docShare.dublincore.description?length &gt; 0 >
        <div class="comment">
          <i class="icon-user"></i>
          <blockquote>${docShare.dublincore.description}</blockquote>
        </div>
      </#if>

      <#if isFolder>
          <a class="action_bar" title="${docShare.title}" href="${basePath}/easyshare/${docShare.id}"> ${Context.getMessage("easyshare.label.backToTheRoot")}</a>
      </#if>

      <div class="shared-items">
        <#list docList as doc>
          <#assign filename=This.getFileName(doc)>

          <#if doc.isFolder>
          <a class="item" title="${filename}" href="${basePath}/easyshare/${docShare.id}/${doc.id}">
            <span class="document">
            <i class="icon-folder"></i>${doc.title}
            </span>
          <#else>
          <a class="item" title="${filename}" target="_blank" href="${basePath}/easyshare/${docShare.id}/${doc.id}/${filename}">
            <span class="document">
            <i class="icon-file"></i>${doc.title}<#if filename != doc.title> - ${filename}</#if>
          </span>
            <i class="icon-download"></i>
          </#if>
        </a>
        </#list>

        <#if !docList>
          <div class="empty"><i class="icon-unhappy"></i>${Context.getMessage("easyshare.label.nofiles")}</div>
        </#if>

    </content>

    <#include "includes/pagination.ftl">

  </div>

  </@block>
</@extends>
