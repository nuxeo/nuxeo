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
    
    <#assign reload=Context.request.getParameter('reload') />
    <#if reload>
      <script type="text/javascript">
          top.navtree.refresh('${bank}-${collection}-style');
      </script>
    </#if>
  </@block>

  <@block name="content">

    <#assign view=skins_only?string("skin", "style") />
    <#assign redirect_url="${Root.getPath()}/${bank}/${collection}/${view}/view" />

    <h1>Style collection: ${collection}
      <a style="float: right" href="${Root.getPath()}/${bank}/${collection}/${view}/view">Refresh</a>
      <#if Root.isAdministrator()>
        <a style="float: right; margin-right: 5px" href="javascript:void(0)" onclick="showStyleCreateForm()">New ${view}</a>
      </#if>
    </h1>

    <#if Root.isAdministrator()>
    <form style="display: none" id="styleCreateForm" action="${Root.path}/${bank}/manage/${collection}/createStyle"
           method="post">
      <h2>Create a new style</h2>
      <p>
        <label>Name</label>
        <strong><input class="textInput" type="text" name="resource" size="20" />.css</strong>
      </p>
      <div>
        <input type="hidden" name="collection" value="${collection}" />
        <input type="hidden" name="redirect_url" value="${redirect_url?replace(' ', '%20')}?reload=1" />
      </div>
      <p>
      <button>Create</button>
        <button onclick="hideStyleCreateForm(); return false">Cancel</button>
      </p>
    </form>
    </#if>


    <div class="album" id="styleGallery">

    <#if skins_only>
      <#list skins as style>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-${collection}-${view}-${style}')">
        <div class="imageSingle">
          <div class="image"><img src="${Root.getPath()}/${bank}/${collection}/style/${style}/preview"></div>
          <div class="footer"><div>${style}</div>
          </div>
        </div>
        </a>
      </#list>

    <#else>
      <#list styles as style>
        <#if !skins?seq_contains(style)>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-${collection}-${view}-${style}')">
        <div class="imageSingle">
          <div class="image"><img src="${Root.getPath()}/${bank}/${collection}/style/${style}/preview"></div>
          <div class="footer"><div>${style}</div>
          </div>
        </div>
        </a>
        </#if>
      </#list>

    </#if>

    </div>

  </@block>

</@extends>
