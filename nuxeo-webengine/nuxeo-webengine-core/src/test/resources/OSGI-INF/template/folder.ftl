
Testing Rendering: ${env.engine} ${env.version}

<#assign ROOT_VAR="a root var"/>
assigned a root var: ${ROOT_VAR}

Document: ${title}
testing this: ${this.path}
<@render/>
Exit sub context

is root var visible? ${ROOT_VAR}
is sub context var visible? ${SUB_VAR}
--------- Testing env list -------
<#list env?keys as key>
ENV > ${key} : ${env[key]}
</#list>


----------------------------------
End.
