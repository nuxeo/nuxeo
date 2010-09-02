<@extends src="base.ftl">

  <@block name="title">
      ${resource}
  </@block>

  <@block name="stylesheets">
    <link type="text/css" rel="stylesheet" href="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shCore.css"/>
    <link type="text/css" rel="stylesheet" href="${basePath}/theme-banks/skin/scripts/syntaxHighlighter/shThemeDefault.css"/>
  </@block>

  <@block name="header_scripts">
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jquery.js"></script>
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jquery.cookie.js"></script>
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/sarissa.js"></script>
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jsTree/jquery.tree.js"></script>

    <script type="text/javascript">
    $(document).ready(function() {

          $("#navtree").tree({
            ui : {
              theme_name: "classic"
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
                        image: "${basePath}/theme-banks/skin/img/bank.png"
                    }
                },
                "folder" : {
                    valid_children: ["collection"],
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/folder.png"
                    }
                },
                "collection" : {
                    valid_children: ["style", "image", "preset"],
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/collection.png"
                    }
                },
                "skins" : {
                    valid_children : "none",
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/skins.png"
                    }
                },
                "style" : {
                    valid_children : "none",
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/style.png"
                    }
                },
                "image" : {
                    valid_children : "none",
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/image.png"
                    }
                },
                "preset" : {
                    valid_children : "none",
                    icon: {
                        image: "${basePath}/theme-banks/skin/img/preset.png"
                    }
                }
            }
        });

    });


    var openBranch = function(id) {
      id = id.replace(/[\s\.]+/g, '-');
      $.tree.focused().select_branch('#' + id);
    }
    </script>
  </@block>

  <@block name="content">
    <div id="navtree"></div>
  </@block>

</@extends>
