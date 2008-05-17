Comments for ${Document.title}


Comments: ${comments?size}

<#list comments as com>
  Comment Title: ${com.title}
  Comment Type -- class: ${com.type} -- ${com.class}
</#list>

<#--
Text: ${com.getPropertyValue('text')} (works with Jython)
Text: ${com["comment:text"]} (work with Groovy)
-->