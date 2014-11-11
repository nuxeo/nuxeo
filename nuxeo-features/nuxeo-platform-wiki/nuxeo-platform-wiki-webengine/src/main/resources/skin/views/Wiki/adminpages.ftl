<@extends src="base.ftl">
<@block name="content">
<html>
<head>
  <title>Admin Pages</title>
  <link rel="stylesheet" type="text/css" href="${skinPath}/css/flexigrid.css" />
  <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/flexigrid.js"></script>
</head>

<body>
  <h1>Admin ${Root.title}'s Pages</h1>

  <table id="pages" style="display:none"></table>

  <script>
function reloadDocs() {
  $('#pages').flexReload();
}

function deleteDoc(com, grid) {
  if (com == 'Delete') {
    doc = $('.trSelected', grid);
    docId = doc.attr('id');
    if (confirm('Are you sure to delete this document?')) {
      $.ajax({
        type: "POST",
        async: false,
        url: '${Root.getUrlPath()?js_string}@@deletePage',
        data: "uuid=" + docId,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          // typically only one of textStatus or errorThrown
          // will have info
          this;
          // the options for this ajax request
          alert("Cannot remove document:" + textStatus + " -- " + errorThrown);
        },
      });
    }
  };
  reloadDocs();
}

$("#pages").flexigrid({
  url: '${Root.UrlPath}@@listPages',
  dataType: 'json',
  buttons : [
    {name: 'Delete', bclass: 'delete', onpress : deleteDoc},
  ],
  colModel : [
    {display: 'Title', name : 'dc:title', width : 350, sortable : true, align: 'center'},
    {display: 'Modified', name : 'dc:modified', width : 150, sortable : true, align: 'center'},
    {display: 'Creator', name : 'dc:creator', width : 100, sortable : true, align: 'left'},
    {display: 'State', name : 'ecm:lifecycle', width : 100, sortable : true, align: 'left'},
  ],
  searchitems : [
    {display: 'Title', name : 'dc:title'},
    {display: 'Fulltext', name : 'ecm:fulltext'},
    {display: 'Creator', name : 'dc:creator'},
  ],
  sortname: "dc:modified",
  sortorder: "desc",
  usepager: true,
  title: 'Wiki Pages',
  useRp: false,
  rp: 20,
  showTableToggleBtn: false,
  width: 750,
  height: 300
});

</script>

</@block>
</@extends>