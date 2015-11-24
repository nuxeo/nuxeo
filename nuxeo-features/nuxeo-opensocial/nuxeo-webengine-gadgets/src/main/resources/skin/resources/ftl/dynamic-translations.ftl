    <#list i18n.supportedLangs as lang>
      <Locale lang="${lang.language}" messages="${specDirectoryUrl}messages_${lang.language}.xml"/>
    </#list>
      <Locale messages="${specDirectoryUrl}messages_en.xml"/>
