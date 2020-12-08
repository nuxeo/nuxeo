<div class="pageNavigationControls">

<@pageLink condition="${previousPageAvailable}" page="0" image="first" />

<@pageLink condition="${previousPageAvailable}" page="${currentPageIndex - 1}" image="previous"/>

  <span class="currentPageStatus"> ${currentPageStatus} </span>

<@pageLink condition="${nextPageAvailable}" page="${currentPageIndex + 1}" image="next" />

<@pageLink condition="${nextPageAvailable}" page="${numberOfPages - 1}" image="last"/>

</div>

<#macro pageLink condition page image>

  <#if condition == "true">
    <#if accessSecured>
    <a href="?p=${page}&a=${encryptedAccessCode}&s=${encryptedSalt}&j=${encryptedJSessionId}">
    <#else>
    <a href="?p=${page}">
    </#if>
  <#else>
  <a disabled="disabled">
  </#if>
  <input type="image" src="${skinPath}/img/navigation_${image}.png">
</a>
</#macro>
