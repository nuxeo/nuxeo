<@extends src="base.ftl">
  <@block name="title">Detail of Space '<#if This.space>${This.space.title}</#if>'</@block>
    <@block name="content">

    <script type="textjavascript" language="javascript" src="/nuxeo/opensocial/gadgets/js/nxcontainer.js?c=1"></script>

    <script type="text/javascript">
     function getGwtParams(){
       return "{docRef:'<#if This.space>${This.space.id}</#if>',sessionId:'${Session.getSessionId()}'}";
     }
     function loading_show(){}
     function loading_remove(){}

    </script>

  <script type="text/javascript" language="javascript" src="/nuxeo/org.nuxeo.opensocial.container.ContainerEntryPoint/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js"></script>



  <#if This.isRoot()>
<h2><#if Root.parent>root parent<#else>Root of all univers</#if></h2>
<#else>
<h2><a href="${This.previous.path}"><img src="${skinPath}/image/up_nav.gif" alt="Up" border="0"/></a> ${This.univers.title}</h2>
</#if>

    <div id="header">

        <h1>Detail of Space '<#if This.space>${This.space.title}</#if>'</h1>
        <ul>
        <#if This.space>
          <#if This.spaces>
           <#list This.spaces as sp>
            <li<#if sp.name=This.space.name> id="selected"</#if>>
              <a href="${Root.path}/<#if This.univers>${This.univers.name}</#if>/${sp.name}">${sp.title}</a>
           </li>
           </#list>
           </#if>
         </#if>
        </ul>

    </div>


  <div id="content">
    <@block name="containerNews" ></@block>
  </div>
  <div id="communicationGadgetContainer" style="display:none">
    <a href="#" id="thickboxContent" class="thickbox" ></a>
  </div>


    <br/><br/>

  <div id="global_actions">

    <h1>Actions</h1>

  <ul>
        <#list This.getLinks("SPACE_ACTIONS") as link>
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
