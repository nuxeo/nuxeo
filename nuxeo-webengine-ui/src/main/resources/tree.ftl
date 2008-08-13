		

<@extends src="base.ftl">

  <@block name="header_scripts">
    <@superBlock/>

    <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.css" type="text/css" media="screen" charset="utf-8">
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery//treeview/jquery.treeview.async.js"></script>

  	<script type="text/javascript">
	
	function doSearch(id) {	 
	  $.get("${basePath}/common/tree.groovy", {id: id},
	  function(data) {
	    $("#my_content").html(data)
	  })
	}

  function doToggle(id) {
    $.get("${appPath}/${id}/common/tree.groovy", {root: id, toggle: true},
    function(data) {
      $("#my_content").html(data)
    })
  }
  
	$(document).ready(function(){
		$("#tree").treeview({
			url: "http://localhost:8080/repository/common/tree.groovy",
      root: "/default-domain"
//			persist: "cookie",
/*			toggle: function (x, item) {
			  //doSearch(this.id)
        doToggle(this.id)
			  //$("#my_content").html("TOGGLED "+this.id+" - "+item.tagName+ " " +item.childNodes.length)
			}*/
		})
	});
	</script>
	

  </@block>
  

  <@block name="content">

    <div id="my_content">This is the content area</div>
  <@block name="sidebar">
    <ul id="tree">
    </ul> 
  </@block>
aaaaa
  </@block>

</@extends>
