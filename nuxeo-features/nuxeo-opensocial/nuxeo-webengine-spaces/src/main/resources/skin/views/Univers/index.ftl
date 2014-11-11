<@extends src="base.ftl">
  <@block name="title">Detail of Univers '${This.univers.title}'</@block>
    <@block name="content">

  <#if This.isRoot()>
<h2><#if Root.parent>${Root.document.title}<#else>Root of all univers</#if></h2>
<#else>
<h2><a href="${This.previous.path}"><img src="${skinPath}/image/up_nav.gif" alt="Up" border="0"/></a> ${This.univers.title}</h2>
</#if>

    <div id="header">


         <#if This.spaces>
           <ul>
             <#list This.spaces as space>
                <li>
                  <a href="${This.path}/${space.name}">${space.title}</a>
               </li>
             </#list>
           </ul>
        </#if>


    </div>

    <div id="content">
      <#if This.spaces>
        <#if This.spaces?size &gt; 0>
          Please select one space
        <#else>
            Please create one space via the create action link above
        </#if>
      </#if>
    </div>

    <br/><br/>

  <div id="global_actions">

    <h1>Actions</h1>

  <ul>
        <#list This.getLinks("UNIVERS_ACTIONS") as link>
        <li>
          <a href="${link.getCode(This)}" title="${link.id}">
            <span>${Context.getMessage(link.id)}</span>
          </a>
         </li>
      </#list>
    </ul>

  </div>




  </@block>

</@extends>
