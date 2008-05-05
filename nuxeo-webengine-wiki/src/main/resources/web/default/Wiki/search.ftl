<#assign q>SELECT * FROM Document WHERE (ecm:fulltext = '${request.getParameter('q')}') AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH '${request.getParameter("p")}')  ORDER BY dc:modified</#assign>

<@extends src="/default/Wiki/base.ftl">

<@block name="content">

<h2>Search results for your query</h2>
    <ul>
    <#list query(q) as doc>
    <li>
        <a href="${this.docURL}/${doc.name}">${doc.title}</a>
            <br/>modified by ${doc.author}
        </li>
    </#list>
    </li>
<br/></br>
<small>Query performed: ${q}</small>
</@block>
</@extends>
