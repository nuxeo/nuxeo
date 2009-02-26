<#macro navigator>

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
      cookieId: "nxnavtree",
    });
  });
</script>

<div class="sideblock general" style="padding: 20px;width: 200px;overflow: auto;">
<div style="display: block; font-size: small" id="navtreecontrol">
    <a title="Collapse the entire tree below" href="#">Collapse All</a> 
    <a title="Expand the entire tree below" href="#">Expand All</a>
</div>
 <div class="treeroot"></div>
  <ul id="treenav" class="treeview" style="font-size: small;">
  </ul>
 </div>
<!-- End Navigation Tree -->
</#macro>
