<#if contextHelper.isMultiRepository()>
<UserPref name="targetRepository" display_name="__MSG__label.target.repository__"
          default_value="${contextHelper.getDefaultRepoName()}"  datatype="enum" required="true">
   <#list contextHelper.getRepoNames() as repoName>
       <EnumValue value="${repoName}" display_value="${contextHelper.getRepoLabel(repoName)}" />
   </#list>
</UserPref>
<#else>
<UserPref name="nuxeoTargetRepository" datatype="hidden" default_value="${contextHelper.getDefaultRepoName()}" />
</#if>
<UserPref name="nuxeoTargetContextPath" datatype="hidden" default_value="/" />
<UserPref name="nuxeoTargetContextObject" datatype="hidden" default_value="Domain" />
