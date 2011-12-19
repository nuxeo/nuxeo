
${simpleVar}

${objectVar.attribute}

${objectVar.method()}

${dateVar?datetime}

[#list items as item]

[/#list]

[#list container.children as item]

[/#list]

[#if condVar1]

[/#if]

[#if condVar2==2]

[/#if]

[#if condVar3??]

[/#if]

[#if condVar4>2]

[/#if]

[#if !condVar5]

[/#if]

[#assign internalVar = 3]

[#if internalVar > 4]

[/#if]

${doc.dublincore.title}

${document.dublincore.description}

[#list auditEntries as auditEntry]

[/#list]

[#if item =='gg']
[/#if]


${doc['dc:title']}

[#list doc['dc:subjects'] as subject]

${subject}

[/#list]

