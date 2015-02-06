<div class="pageNavigationControls">

<@pageLink condition="${previousPageAvailable}" page="0" image="first" />

<@pageLink condition="${previousPageAvailable}" page="${currentPageIndex - 1}" image="previous"/>

  <span class="currentPageStatus"> ${currentPageStatus} </span>

<@pageLink condition="${nextPageAvailable}" page="${currentPageIndex + 1}" image="next" />

<@pageLink condition="${nextPageAvailable}" page="${numberOfPages}" image="last"/>

</div>

<#macro pageLink condition page image>

  <#if condition == "true">
  <a href="?p=${page}">
  <#else>
  <a disabled="disabled">
  </#if>
  <input type="image" src="${skinPath}/img/navigation_${image}.png">
</a>
</#macro>
