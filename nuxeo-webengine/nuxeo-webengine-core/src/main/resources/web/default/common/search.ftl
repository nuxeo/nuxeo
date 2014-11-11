
<#assign path>${Request.getParameter("p")}</#assign>

<#if path = "">
  <#if Context.hasTraversalPath()=true >
    <#if Document.isFolder >
      <#assign path>${Document.path}</#assign>    
    <#else>
      <#assign path>${Document.parent.path}</#assign>    
    </#if>
  </#if>
</#if>

<#if path="">
  <#assign q>SELECT * FROM Document WHERE (ecm:fulltext = "${Request.getParameter('q')}") AND (ecm:isCheckedInVersion = 0) ORDER BY dc:modified</#assign>
<#else>
  <#assign q>SELECT * FROM Document WHERE (ecm:fulltext = "${Request.getParameter('q')}") AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH "${path}")  ORDER BY dc:modified</#assign>
</#if>


<@extends src="base.ftl">

<@block name="content">

<h2>Search results for your query</h2>
    <ul>
    <#list Session.query(q) as doc>
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
