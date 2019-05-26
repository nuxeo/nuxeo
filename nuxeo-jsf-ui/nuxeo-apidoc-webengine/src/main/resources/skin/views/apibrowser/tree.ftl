<@extends src="base.ftl">

<@block name="stylesheets">
</@block>

<@block name="header_scripts">
    <@mod_tree />
</@block>

<#macro mod_tree>
    <link rel="stylesheet" href="${skinPath}/script/jquery//treeview/jquery.treeview.css" type="text/css" media="screen" charset="utf-8">
    <script type="text/javascript" src="${skinPath}/script/jquery//treeview/jquery.treeview.js"></script>
    <script type="text/javascript" src="${skinPath}/script/jquery//treeview/jquery.treeview.async.js"></script>
</#macro>

<#macro tree id url="${Root.path}/${distId}/tree" root="/">
  <script type="text/javascript">
  $(document).ready(function(){
    $("#${id}").treeview({
      url: "${url}",
      root: "${root}",
      animated: "fast",
      unique: true
    });
  });
  </script>

  <ul id="${id}" class="filetree">
  </ul>

</#macro>

<@block name="right">

 <h1> Test Tree</h1>

 <@tree id="myTree" root="/"/>

</@block>

</@extends>
