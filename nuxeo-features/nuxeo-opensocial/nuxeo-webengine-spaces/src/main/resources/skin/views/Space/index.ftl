<@extends src="base.ftl">

<@block name="header_scripts">
  <@superBlock/>
  <script>var nxBaseUrl = "${This.baseUrl}"</script>
</@block>


<@block name="content">
<div>
  <div style="min-height:300px;height:auto !important;height:300px;">
    <link rel="stylesheet" href="${skinPath}/css/gadgetContainer.css" type="text/css" media="screen" charset="utf-8" />
    <script type="text/javascript" language="javascript" src="${contextPath}/opensocial/gadgets/js/rpc.js?c=1"></script>
    <script type="text/javascript">
     function getGwtParams(){
       return "{docRef:'<#if This.space>${This.space.id}</#if>', nxBaseUrl: '${This.baseUrl}'}";
     }
  </script>
  <script type="text/javascript" language="javascript" src="${contextPath}/org.nuxeo.opensocial.container.ContainerEntryPoint/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js"></script>
    <div id="content">
            <div id="gwtContainer"/>
    </div>
  </div>
</div>
</@block>
</@extends>