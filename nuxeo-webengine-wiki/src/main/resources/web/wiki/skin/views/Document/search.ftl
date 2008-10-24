
<@extends src="base.ftl">

<@block name="content">

<h2>Search results for your query</h2>
    <ul>
    <#list result as doc>
    <li>
        <a href="${Context.getUrlPath(doc)}">${doc.dublincore.title}</a>
            <br/>modified by ${doc.dublincore.creator}
        </li>
    </#list>
    </li>
<br/></br>
<small>Query performed: ${query}</small>
</@block>
</@extends>
