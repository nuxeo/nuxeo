<div>
<style>
     li.pageAction{
       list-style-type:none;
       padding:2px;
      }
     li.pageAction a{
      color:rgb(0, 0, 0);
       text-decoration:none;
       font-weight:bold;
       margin:4px;
      }
      ul.menu-ul{
       border:1px solid black;
        background-color:rgb(255, 255, 255);
        margin:0;
        width:130px;
      }
</style>
  <ul class="listOnglets">
    <#list This.spaces as currentSpace>
      <li class="onglet tab_0 <#if This.space ><#if This.space.id == currentSpace.id >current</#if></#if> accueil">

        <#if This.space>
          <#if This.space.id != currentSpace.id >
            <a title="${currentSpace.description}" class="accueil"  href="${This.previous.path}/${currentSpace.name}">
              ${currentSpace.title}
            </a>
          <#else>
            <span class="accueil" id="currentOngletName" title="${currentSpace.description}">${currentSpace.title}</span>
          </#if>
        </#if>
      </li>
    </#list>
     <script>$(document).ready(function(){
tb_init("a.nav-tabs-thickbox");
});</script>


    <!--#if nxthemesInfo.model.anonymous == false-->

       <!--li class="onglet tab_0">
         <a id="addTabLink" title="Ajouter un nouvel onglet" href="{Context.root.path}/{This.univers.name}/@views/create?theme=intralm/popup&amp;height=300&amp;width=300" class="thickbox">+</a>
       </li-->


     <!--/#if-->


  </ul>
</div>