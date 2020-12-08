<@extends src="base.ftl">

  <@block name="content">

  <div>
    <div class="denied"><i class="icon-unhappy"></i>
<#if wrongAccessCode >
${Context.getMessage("easyshare.label.wrongAccessCode")}
<#else>
${Context.getMessage("easyshare.label.denied")}
</#if>
<br><br>
<#if doc.id?length &gt; 0 >
  <#if doc.id == docShare.id >
  <form method="POST" action="${basePath}/easyshare/${docShare.id}" onSubmit="" id="easyshareAccess">
  <#else>
  <form method="POST" action="${basePath}/easyshare/${docShare.id}/${doc.id}" onSubmit="" id="easyshareAccess">
  </#if>
<#else>
  <form method="POST" action="${basePath}/easyshare/${docShare.id}" onSubmit="" id="easyshareAccess">
</#if>

    ${Context.getMessage("easyshare.label.enterAccessCode")} <input type="password" name="accessCode" value="" />
    <input type="hidden" name="shareId" value="${docShare.id}" />
    <input type="hidden" name="folderOrFileId" value="${doc.id}" />
    <input type="hidden" name="filename" value="${filename}" />
    <input type="submit" value="${Context.getMessage("easyshare.label.submit")}" />
  </form>
</div></div>
  </@block>
</@extends>
