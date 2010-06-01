<@extends src="base.ftl">

  <@block name="title">${bank} skins</@block>

  <@block name="header_scripts">
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jquery.js"></script>
    <script type="text/javascript">
      $.getJSON('${Root.getPath()}/${bank}/json/skins', function(data) {
      for (i=0; i < data.length; i++) {
        var skin = data[i];
        $('#skins').append(
          '<div class="imageSingle"><div class="image">' +
          '<img src="${Root.getPath()}/' + skin.bank + '/style/' + skin.collection + '/' + skin.resource + '/preview">' +
          '</div><div class="footer">' + skin.resource + '</div></div>'
        )
      }
    });
    </script>
  </@block>
  
  <@block name="content">
  
    <h1><a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}')">${bank}</a> &gt;  
        <span>skins</span></h1>
  
    <div id="skins" class="album">
    </div>
  </@block>

</@extends>
