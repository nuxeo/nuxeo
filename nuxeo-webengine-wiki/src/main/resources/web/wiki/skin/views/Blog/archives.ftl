<@extends src="Blog/base.ftl">
<@block name="content">

<h1>Archives for
    <#if month>${sdate?string("MMMM yyyy")}
    <#else>${sdate?string("yyyy")}</#if></h1>

${sdate?date}

<br/>
<br/>

<#list results as doc>
    ${doc.dublincore.title}
</#list>

<br/><br/>
<small>${pquery}</small>

</@block>
</@extends>