
Testing Rendering: ${env.engine} ${env.version}

<#assign ROOT_VAR="a root var"/>
assigned a root var: ${ROOT_VAR}

Document: ${title}
Dublincore title: ${dublincore.title}
testing this: ${this.path}
<@render/>
Exit sub context

is root var visible? ${ROOT_VAR}
is sub context var visible? ${SUB_VAR}
--------- Testing env list -------
<#list env?keys as key>
ENV > ${key} : ${env[key]}
</#list>


Some wiki inside:
<@transform name="wiki">
  This is a wiki name: MyName
  This is a JIRA link: NXP-1234
  This is *bold*

  And a ~paragraph~

  and some items from a list:
+ item1
+ item2
</@transform>
<@transform name="wiki">
${dublincore.description}
</@transform>

<@transform name="wiki">
${dublincore.content.data}
</@transform>


----------------------------------
End.
