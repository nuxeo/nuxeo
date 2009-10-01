<@extends src="base.ftl">

<@block name="title">Detail of Gadget '${This.gadget.title}'</@block>
<@block name="content">

  <#if This.isRoot()>
<h2><#if Root.parent>${Root.document.title}<#else>Root of all univers</#if></h2>
<#else>
<h2><a href="${This.previous.path}"><img src="${skinPath}/image/up_nav.gif" alt="Up" border="0"/></a> ${This.univers.title}</h2>
</#if>

Gadget >  ${This.gadget.title}


  <div id="global_actions">

    <h1>Actions</h1>

  <ul>
        <#list This.getLinks("GADGET_ACTIONS") as link>
        <li>
          <a href="${link.getCode(This)}" title="${link.id}">
            <span>${Context.getMessage(link.id)}</span>
          </a>
         </li>
      </#list>
    </ul>

</@block>
</@extends>