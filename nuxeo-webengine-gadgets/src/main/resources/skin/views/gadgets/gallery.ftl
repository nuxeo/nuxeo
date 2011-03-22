<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
  <title>Available Gadgets</title>
  <link type="text/css" rel="stylesheet" href="${skinPath}/css/gadgets-gallery.css" />

  <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="${skinPath}/scripts/gadgets-gallery.js"></script>

  <script type="text/javascript">
    var galleryBaseUrl = '${This.path}';
  </script>
</head>
<body>

<div id="gadgetsListContainer">
  <h1>Available Gadgets</h1>
  <table class="gadgetBrowser">
    <tr>
      <td>
        <div class="categoryList">
          <ul>
            <#list categories as cat>
            <#if cat == category>
            <li class="category currentCategory">
              <a onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
            <#else>
            <li class="category">
              <a onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
            </#if>
              ${This.getCategoryLabel(cat)}
              </a>
            </li>
            </#list>
          </ul>
        </div>
      </td>
      <td>
      <div id="gadgetListContainer">
        <#include "/views/gadgets/list.ftl">
      </div>
      </td>
    </tr>
  </table>
</div>
</body>
</html>
