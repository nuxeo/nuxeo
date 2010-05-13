<html>
<head>
    <title>Nuxeo Theme Bank</title>

    <link type="text/css" rel="stylesheet" href="${skinPath}/styles/ui.css" />
        
    <script type="text/javascript" src="${skinPath}/scripts/jquery.js"></script>
    <script type="text/javascript" src="${skinPath}/scripts/jquery.cookie.js"></script>
    <script type="text/javascript" src="${skinPath}/scripts/sarissa.js"></script>
    <script type="text/javascript" src="${skinPath}/scripts/jsTree/jquery.tree.js"></script>

    <script type="text/javascript">
    $(document).ready(function() {
    
          $("#navtree").tree({
            ui : {
              theme_name : "classic"
            },
            data : { 
                type : "json",
                opts : {
                    url : "${Root.getPath()}/${bank}/json/tree"
                }
            },
            callback : {
                onselect: function(node, tree_obj) { 
                    tree_obj.open_branch.call(tree_obj, node);
                    console.log(tree_obj.get_text(node));
                    console.log(tree_obj.get_type(node));
                    var path = $(node).attr('path');
                    $("#main").load("${Root.getPath()}" + path.replace(/\s/g, '%20') + '/view');
                }
            },
            types : {
                "default" : {
                    clickable : true,
                    deletable : false,
                    renameable : false,
                    draggable : false
                },
                "bank" : {
                    valid_children: ["folder"],
                    icon: {
                        image: "${skinPath}/img/bank.png"
                    }
                },
                "folder" : {
                    valid_children: ["collection"],                
                    icon: {
                        image: "${skinPath}/img/folder.png"
                    }
                },                
                "collection" : {
                    valid_children: ["style", "image", "preset"],                 
                    icon: {
                        image: "${skinPath}/img/collection.png"
                    }
                },
                "style" : {
                    valid_children : "none",
                    icon: {
                        image: "${skinPath}/img/style.png"
                    }
                },
                "image" : {
                    valid_children : "none",
                    icon: {
                        image: "${skinPath}/img/image.png"
                    }
                },
                "preset" : {
                    valid_children : "none",
                    icon: {
                        image: "${skinPath}/img/preset.png"
                    }
                }
            }
        });
        
    });
    </script>
    
  
</head>

<body>
  <h1>Nuxeo Theme Bank</h1>

  <table class="screen">
  <tr>
  <td class="navtree">
    <div id="navtree"></div>
  </td>
  <td class="main">
    <div id="main"></div>
  </td>
  </tr>
  </table>
</body>
</html>