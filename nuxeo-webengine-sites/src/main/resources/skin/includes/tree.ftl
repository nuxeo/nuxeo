<#macro navigator>
<h4>${Context.getMessage("label.tree")}</h4>
<!-- Navigation Tree -->
<link rel="stylesheet" href="${skinPath}/script/jquery/treeview/demo/screen.css" type="text/css" media="screen"/>
<link rel="stylesheet" href="${skinPath}/script/jquery/treeview/jquery.treeview.css" type="text/css" media="screen"/>
<link rel="stylesheet" href="${skinPath}/script/jquery/treeview/red-treeview.css" />
<script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
<script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>
<script type="text/javascript" src="${skinPath}/script/jquery/treeview/jquery.treeview.js"></script>
<script type="text/javascript" src="${skinPath}/script/jquery/treeview/jquery.treeview.async.js"></script>
<script>
  $(document).ready(function() {
    $('#treenav').treeview({
      url: "${This.path}/@json",
      persist: "cookie",
      control: "#navtreecontrol",
      //collapsed: false,
      cookieId: "nxnavtree"
    });
  });
</script>

<div class="sideblock general">
 <div class="treeroot"></div>
  <ul id="treenav" class="treeview">
  </ul>
</div>
<!-- End Navigation Tree -->
</#macro>
