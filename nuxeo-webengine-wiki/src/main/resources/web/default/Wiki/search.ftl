<#assign q>SELECT * FROM Document WHERE (ecm:fulltext = '${Request.getParameter('q')}') AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH '${Request.getParameter("p")}')  ORDER BY dc:modified</#assign>

<@extends src="/default/Wiki/base.ftl">

<@block name="content">

<h2>Search results for your query</h2>
    <ul>
    <#list query(q) as doc>
    <li>
        <a href="${This.urlPath}/${doc.name}">${doc.title}</a>
            <br/>modified by ${doc.author}
        </li>
    </#list>
    </li>
<br/></br>
<small>Query performed: ${q}</small>
</@block>
</@extends>
