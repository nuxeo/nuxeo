<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
  <title>${Context.getMessage('label.gadgets.selection.title')}</title>
  <link type="text/css" rel="stylesheet" href="${skinPath}/css/gadgets-gallery.css" />

  <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="${skinPath}/scripts/gadgets-gallery.js"></script>

  <script type="text/javascript">
    var galleryBaseUrl = '${This.path}';
    var language = '${This.context.locale.language?js_string?html}';
  </script>
</head>
<body>

<div id="gadgetsListContainer">
  <h1>${Context.getMessage('label.gadgets.selection.title')}</h1>
  <table class="gadgetBrowser">
    <tr>
      <td class="gadgetCategories">
        <div class="categoryList">
          <ul>
            <#list categories as cat>
            <li class="category<#if cat == category> currentCategory</#if>">
              <a href="#" category-name="${cat}">
              ${This.getCategoryLabel(cat)}
              </a>
            </li>
            </#list>
          </ul>
        </div>
      </td>
      <td class="gadgetList">
      <div id="gadgetListContainer">
        <#include "/views/gadgets/list.ftl">
      </div>
      </td>
    </tr>
  </table>
</div>
</body>
</html>
