<@extends src="base.ftl">

<@block name="stylesheets">
 <@superBlock/>
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/fancybox.css" type="text/css" media="screen" charset="utf-8">
</@block>

<@block name="header_scripts">
 <@superBlock/>
<script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
<script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/jquery.fancybox.js"></script>
<script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/jquery.metadata.js"></script>  
</@block>

<@block name="content">
<p>
<b>Name:</b> ${Document.name}
<br/>
<b>Type:</b> ${Document.type}
<br/>
<b>Id:</b> ${Document.id}
</p>
<p>
${Document.description}
</p>
<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

<#if Document.facets?seq_contains("Folderish")>
  <ul>
  <p id="book_images">
  <#list Document.children as child>
    <A href="/nuxeo/nxpicsfile/default/${child.id}/webengine:content/?inline=true" rel="book" title="${child.title}">
        <img src="/nuxeo/nxpicsfile/default/${child.id}/Thumbnail:content/?inline=true"/>
    </A>
  </#list>
  </ul>
  <hr/>
</#if>

<script>
$(document).ready(function() { $("p#book_images a").fancybox({'overlayShow': true} ); }); 
</script>
</@block>
</@extends>
