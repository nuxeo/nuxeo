<#if contextHelper.isMultiRepository()>
<UserPref name="targetRepository" display_name="__MSG__label.target.repository__"
          default_value="${contextHelper.getDefaultRepoName()}"  datatype="enum" required="true">
   <#list contextHelper.getRepoNames() as repoName>
       <EnumValue value="${repoName}" display_value="${contextHelper.getRepoLabel(repoName)}" />
   </#list>
</UserPref>
<#else>
<UserPref name="targetRepository" default_value="${contextHelper.getDefaultRepoName()}"  datatype="hidden"/>
</#if>
<UserPref name="targetContextPath" datatype="hidden" default_value="default-domain"/>
<UserPref name="targetContextObject" datatype="hidden" default_value="Domain"/>
