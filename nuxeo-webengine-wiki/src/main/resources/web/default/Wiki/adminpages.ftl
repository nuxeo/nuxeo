<html>
<head>
    <title>Admin Pages</title>
    <link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/css/flexigrid.css">
<script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
<script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/flexigrid.js"></script>

</head>


<body>
    <h1>Admin Pages</h1>
    
<table id="flex1" style="display:none"></table>

<script>
$()
$("#flex1").flexigrid
			(
			{
			url: '${Root.UrlPath}@@listpages',
			dataType: 'json',
			colModel : [
				{display: 'Title', name : 'dc:title', width : 250, sortable : true, align: 'center'},
				{display: 'Modified', name : 'dc:modified', width : 150, sortable : true, align: 'center'},
				{display: 'Creator', name : 'dc:creator', width : 100, sortable : true, align: 'left'},
				{display: 'State', name : 'ecm:lifecycle', width : 100, sortable : true, align: 'left'},
				],
			searchitems : [
				{display: 'Title', name : 'dc:title'},
				{display: 'Modified', name : 'dc:modified'},
				{display: 'Fulltext', name : 'ecm:fulltext'},
				],
			sortname: "dc:modified",
			sortorder: "desc",
			usepager: true,
			title: 'Wiki Pages',
			useRp: true,
			rp: 20,
			showTableToggleBtn: true,
			width: 750,
			height: 300
			}
			);   

</script>


</body>