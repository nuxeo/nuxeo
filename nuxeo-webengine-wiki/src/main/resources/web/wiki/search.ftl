<#assign q>SELECT * FROM Document WHERE (ecm:fulltext = "${Request.getParameter('q')}") AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH "${Request.getParameter("p")}")  ORDER BY dc:modified</#assign>


<@extends src="Wiki/base.ftl">

<@block name="content">

<h2>Search results for your query</h2>
    <ul>
    <#list Context.search(q) as doc>
    <li>
        <a href="${Context.getUrlPath(doc)}">${doc.dublincore.title}</a>
            <br/>modified by ${doc.dublincore.creator}
        </li>
    </#list>
    </li>
<br/></br>
<small>Query performed: ${q}</small>
</@block>
</@extends>
