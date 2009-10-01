<div>
<@block name="main-content">
<div>
  <div style="min-height:300px;height:auto !important;height:300px;">
    <script type="text/javascript" language="javascript" src="/nuxeo/opensocial/gadgets/js/rpc.js?c=1"></script>
    <script type="text/javascript">
     function getGwtParams(){
       return "{docRef:'<#if This.space>${This.space.id}</#if>',sessionId:'${Session.getSessionId()}'}";
     }
  </script>
  <script type="text/javascript" language="javascript" src="${basePath}/gadgets/resources/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js"></script>
    <div id="content">
            <div id="gwtContainer" />
    </div>
  </div>
</div>
 <script type="text/javascript">
    loading_show();

    <#if nxthemesInfo.model.isAnonymous == true>
     $(document).ready( function() {
       loading_remove();
       });
    </#if>


  </script>
</@block>
</div>
