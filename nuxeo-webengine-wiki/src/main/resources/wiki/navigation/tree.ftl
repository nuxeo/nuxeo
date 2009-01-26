<#macro navtree rootdoc>
<#assign rooturl=Context.getUrlPath(rootdoc) />

<!-- Navigation Tree -->
<link rel="stylesheet" href="${skinPath}/script/jquery/treeview/jquery.treeview.css" type="text/css" media="screen"/>
<script type="text/javascript" src="${skinPath}/script/jquery/treeview/jquery.treeview.js"></script>
<script type="text/javascript" src="${skinPath}/script/jquery/treeview/jquery.treeview.async.js"></script>
<script>
  $(document).ready(function() {
    $('#treenav').treeview({
      url: "${rooturl}@@children",
      persist: "cookie",
      control: "#navtreecontrol",
      //collapsed: false,
      cookieId: "nxnavtree",
    });
  });
</script>

<div class="sideblock general">
  <h3>Navigation</h3>
  <div style="display: block;" id="navtreecontrol">
    <a title="Collapse the entire tree below" href="#">Collapse All</a>
    <a title="Expand the entire tree below" href="#">Expand All</a>
    <a title="Toggle the tree below, opening closed branches, closing open branches" href="#">Toggle All</a>
  </div>
  <div class="treeroot"><a href="${rooturl}">${rootdoc.title}</a></div>
  <ul id="treenav" class="treeview"></ul>
</div>
<!-- End Navigation Tree -->
</#macro>