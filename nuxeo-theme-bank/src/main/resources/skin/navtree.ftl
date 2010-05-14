<@extends src="base.ftl">

  <@block name="title">
      ${resource}
  </@block>

  <@block name="stylesheets">
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shCore.css"/>
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shThemeDefault.css"/>
  </@block>
  
  <@block name="header_scripts">
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
                    var path = $(node).attr('path');
                    parent.frames['main'].location='${Root.getPath()}' + path + '/view';
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
  </@block>
  
  <@block name="content">
    <div id="navtree"></div>  
  </@block>

</@extends>
