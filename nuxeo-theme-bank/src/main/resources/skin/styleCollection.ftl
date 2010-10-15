<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="header_scripts">
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jquery.js"></script>
    <script type="text/javascript">
      var showStyleCreateForm = function (){
        $('#styleCreateForm').show();
        $('#styleGallery').hide();
        return false;
      }
      var hideStyleCreateForm = function (){
        $('#styleCreateForm').hide();
        $('#styleGallery').show();
        return false;
      }
    </script>
  </@block>

  <@block name="content">

    <#assign redirect_url="${Root.getPath()}/${bank}/style/${collection}/view" />

    <h1>Style collection: ${collection}
      <a style="float: right" href="${Root.getPath()}/${bank}/style/${collection}/view">Refresh</a>
      <#if (Context.principal)>
        <a style="float: right; margin-right: 5px" href="javascript:void(0)" onclick="showStyleCreateForm()">New style</a>
      </#if>
    </h1>

    <#if (Context.principal)>
    <form style="display: none" id="styleCreateForm" action="${Root.path}/${bank}/manage/createStyle"
           method="post">
      <h2>Create a new style</h2>
      <p>
        <label>Name</label>
        <strong><input class="textInput" type="text" name="resource" size="20" />.css</strong>
      </p>
      <div>
        <input type="hidden" name="collection" value="${collection}" />
        <input type="hidden" name="redirect_url" value="${redirect_url?replace(' ', '%20')}" />
      </div>
      <p>
      <button>Create</button>
        <button onclick="hideStyleCreateForm(); return false">Cancel</button>
      </p>
    </form>
    </#if>

    <div class="album" id="styleGallery">
      <#list styles as style>
        <#assign style_info=info[style] />
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-style-${collection}-${style}')">
        <div class="imageSingle">
          <div class="image"><img src="${Root.getPath()}/${bank}/style/${collection}/${style}/preview"></div>
          <div class="footer"><div>${style}
            <#if style_info>
               (${style_info.description})
            </#if>
            </div>
          </div>
        </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>
