

<@extends src="base.ftl">

  <#macro mod_tree>
    <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.css" type="text/css" media="screen" charset="utf-8">
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.async.js"></script>
  </#macro>

  <#macro tree id url="${appPath}/common/tree.groovy" root="/default-domain">

    <script type="text/javascript">

  $(document).ready(function(){
    $("#${id}").treeview({
      url: "${url}",
      root: "${root}"
    })
  });
  </script>
    <ul id="${id}">
    </ul>
  </#macro>


  <@block name="header_scripts">
    <@superBlock/>
    <@mod_tree />
  </@block>


  <@block name="content">
    <@tree id="myTree" root="/default-domain"/>
  </@block>

</@extends>
