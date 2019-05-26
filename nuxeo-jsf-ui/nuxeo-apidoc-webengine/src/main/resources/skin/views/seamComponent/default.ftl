<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Seam component ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Seam component <span class="componentTitle">${nxItem.name}</span></h1>

<div class="tabscontent">

  <h2>Documentation</h2>
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
  <#if Root.canAddDocumentation()>
    <div class="tabsbutton">
      <a class="button" href="${This.path}/doc">Manage Documentation</a>
    </div>
  </#if>

  <h2>Scope</h2>
  ${nxItem.scope}

  <h2>Implementation</h2>
  <p><b>${nxItem.className}</b></p>
  <p><div id="shortjavadocimpl" class="description"></div></p>
  <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(nxItem.className)}"/>
  <#assign urlBaseImpl="${javaDocBaseUrl}/javadoc/${nxItem.className?replace('.','/')}"/>
  <p><a href="${urlBaseImpl}.html" target="_new">Click for full Javadoc</a></p>

  <#assign hasInterface=false/>
  <#list nxItem.interfaceNames as iface>
    <#if iface != nxItem.className>
      <#assign hasInterface=true/>
      <#break>
    </#if>
  </#list>

  <#if hasInterface>
  <h2>Interfaces</h2>
  <ul>
    <#list nxItem.interfaceNames as iface>
    <#if iface != nxItem.className>
    <li>
      <p><b>${iface}</b></p>
      <p><div id="shortjavadociface${iface_index}" class="description"></div></p>
      <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(iface)}"/>
      <#assign urlBase="${javaDocBaseUrl}/javadoc/${iface?replace('.','/')}"/>
      <p><a href="${urlBase}.html" target="_new">Click for full Javadoc</a></p>
      <script type="text/javascript">
        $(document).ready(function() {
          $.ajax({
            url: "${Root.path}/../../ajaxProxy?type=text&url=${urlBase?url}.type.html",
            dataType: "text",
            success: fixJavaDocPaths('#shortjavadociface${iface_index}', '${javaDocBaseUrl}')
          });
        });
      </script>
    </li>
    </#if>
    </#list>
  </ul>
  </#if>

  <@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</div>

</@block>

<@block name="footer_scripts">
<script type="text/javascript">
  $(document).ready(function() {
    $.ajax({
      url: "${Root.path}/../../ajaxProxy?type=text&url=${urlBaseImpl?url}.type.html",
      dataType: "text",
      success: fixJavaDocPaths("#shortjavadocimpl", '${javaDocBaseUrl}')
    });
  });
</script>
</@block>

</@extends>
