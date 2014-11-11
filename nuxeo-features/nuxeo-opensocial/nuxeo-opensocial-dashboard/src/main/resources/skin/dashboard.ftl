<html>
<head>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/jquery-1.3.2.min.js"></script>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/jquery.cookie.js"></script>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/jquery.menu.js"></script>

    <script type="text/javascript" language="javascript" src="${This.path}/resources/loading.js"></script>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/thickbox.js"></script>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/initMenu.js"></script>

    <script type="text/javascript" language="javascript" src="${This.path}/resources/layoutManager.js"></script>
    <script type="text/javascript" language="javascript" src="${This.path}/resources/gadgetManager.js"></script>

    <script type="text/javascript">
     function getgwtparams(){
       return "{docref:'<#if This.dashboard>${This.dashboard.id}</#if>',sessionId:'${Session.getSessionId()}'}";
     }
     function loading_show(){}
     function loading_remove(){}


    </script>
  <link rel="stylesheet" type="text/css" href="${This.path}/resources/container.css" />
  <link rel="stylesheet" type="text/css" href="${This.path}/resources/dashboard.css" />
  <script type="text/javascript" language="javascript" src="/nuxeo/org.nuxeo.opensocial.container.ContainerEntryPoint/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js"></script>
</head>
<body>
    <div id="communicationGadgetContainer" style="display:none">
        <a href="#" id="thickboxContent" class="thickbox" ></a>
    </div>

  <div id="getLayoutManager">
    <a href="#" id="openLayoutManager">Modifier l'apparence</a>
  </div>
  <div id="layoutManager" style="display:none;">
    <div id="closeLayoutManager">
      <a href="#" id="closeLinkLayoutManager">Fermer</a>
    </div>
    <div id="layoutManagerContainer">
      <div id="listColumns">
        <div class="button-container">
          <button class="nv-layout" box="1">1</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="2">2</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="3">3</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="4">4</button>
        </div>
      </div>
      <div id="listLayouts">
        <ul class="option" id="tabLayout">
          <li class="invisible">
            <a href="#_" id="x-1-default" name="x-1-default" class="typeLayout" box="1"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-2-default" name="x-2-default" class="typeLayout" box="2"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-3-default" name="x-3-default" class="typeLayout" box="3"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-3-header2cols" name="x-3-header2cols" class="typeLayout" box="3"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-footer3cols" name="x-4-footer3cols" class="typeLayout" box="4"></a>
          </li>
        </ul>
      </div>
    </div>

  </div>

  <div id="getGadgetManager">
    <a href="#" id="openGadgetManager">Ajouter un gadget</a>
  </div>
  <div id="gadgetManager" style="display:none;">

    <div id="closeGadgetManager">
      <a href="#" id="closeLinkGadgetManager">Fermer</a>
    </div>
    <div id="gadgetManagerContainer">
      <div id="listCategories">
      <#list This.categories as category>
          <div class="button-container">
              <!--button class="nv-category" category="${category}">${category}</button-->
              <a class="nv-category" category="${category}">${category}</a>
          </div>
       </#list>
      </div>
      <div id="listGadgets">
        <ul class="option" id="tabGadgets">
        <#list This.gadgets as gadget>
          <li class="invisible">
            <div class="nameGadget">${gadget.name}</div>
            <a href="#_" name="${gadget.name}" class="typeGadget" category="${gadget.category}" style="background-image:url(${gadget.iconUrl})"></a>
            <div class="directAdd">
              <a href="#" class="directAddLink linkAdd" name="${gadget.name}">
                  <span class="addGadgetButton">ajouter</span>
              </a>
          </div>
          </li>
       </#list>
      </ul>
      </div>
    </div>

  </div>

    <!--main content-->
  <div>
  <div style="min-height:300px;height:auto !important;height:300px;">
    <script type="text/javascript" language="javascript" src="/nuxeo/opensocial/gadgets/js/rpc.js?c=1"></script>
    <script type="text/javascript">
     function getGwtParams(){
       return "{docRef:'<#if This.space>${This.space.id}</#if>',sessionId:'${Session.getSessionId()}'}";
     }
  </script>
  <script type="text/javascript" language="javascript" src="/nuxeo/org.nuxeo.opensocial.container.ContainerEntryPoint/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js"></script>
    <div id="content">
            <div id="gwtContainer" />
    </div>
  </div>
</div>


</body>
</html>