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
        <#if accessSecured>
          <a class="action_bar" title="${docShare.title}" href="${basePath}/easyshare/${docShare.id}?a=${encryptedAccessCode}&s=${encryptedSalt}&j=${encryptedJSessionId}"> ${Context.getMessage("easyshare.label.backToTheRoot")}</a>
        <#else>
          <a class="action_bar" title="${docShare.title}" href="${basePath}/easyshare/${docShare.id}"> ${Context.getMessage("easyshare.label.backToTheRoot")}</a>
        </#if>
      </#if>

      <div class="shared-items">
        <#list docList as doc>
          <#assign filename=This.getFileName(doc)>

          <#if doc.isFolder>
            <#if accessSecured>
              <a class="item" title="${filename}" href="${basePath}/easyshare/${docShare.id}/${doc.id}?a=${encryptedAccessCode}&s=${encryptedSalt}&j=${encryptedJSessionId}">
            <#else>
              <a class="item" title="${filename}" href="${basePath}/easyshare/${docShare.id}/${doc.id}">
            </#if>
            <span class="document">
            <i class="icon-folder"></i>${doc.title}
            </span>
          <#else>
            <#if accessSecured>
              <a class="item" title="${filename}" target="_blank" href="${basePath}/easyshare/${docShare.id}/${doc.id}/${filename}?a=${encryptedAccessCode}&s=${encryptedSalt}&j=${encryptedJSessionId}">
            <#else>
              <a class="item" title="${filename}" target="_blank" href="${basePath}/easyshare/${docShare.id}/${doc.id}/${filename}">
            </#if>
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
