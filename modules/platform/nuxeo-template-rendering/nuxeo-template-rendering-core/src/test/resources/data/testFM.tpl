
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

[#list (doc['dc:subjects']) as subject]

${subject}

[/#list]

<text:text-input text:description="">[#list auditEntries as auditEntry]</text:text-input></text:p><text:p text:style-name="P1"/><text:p text:style-name="P3"><text:span text:style-name="T1">
<text:text-input text:description="">${auditEntry.eventId}</text:text-input></text:span></text:p><text:p text:style-name="P2"/><text:p text:style-name="P2"><text:text-input text:description="">[/#list]</text:text-input>

${functions.getVocabularyTranslatedLabel(vocname, doc['dc:nature'], "en")}
${functions.formatDate(doc['dc:created'])}
${functions.formatDate(doc.dublincore.created)}

${core.getParent().dublincore.title}

[#if (doc.schema.field ==1)]
[/#if]

[#if (doc.schema.field > 1)]
[/#if]

[#if (doc.schema.field &gt; 0)]
[/#if]

[#list 1..(doc.vItemGeneral.packMaterialNature?size - 1) as cpt]
[/#if]

[#list ((doc.vItemCaracteristics.allergens)![]) as allergen]
  ${functions.getVocabularyTranslatedLabel("Allergens",allergen.allergen,"fr")}
  ${allergen.quantity}
  ${(functions.getVocabularyLabel("AllergenUnit",allergen.quantityUnit))!}
[/#list]

[#list (doc['dc:subjects']) as subject]
 [#if subject_has_next]
  ${subject_index}
 [/#if]
[/#list]

${.lang}
