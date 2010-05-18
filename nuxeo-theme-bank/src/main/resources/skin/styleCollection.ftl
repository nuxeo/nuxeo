<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1><a href="${Root.getPath()}/${bank}/view">${bank}</a> &gt; <a href="${Root.getPath()}/${bank}/style/view">style</a> &gt; ${collection}</h1>    
    <div class="album">
      <#list styles as style>
        <a href="${Root.getPath()}/${bank}/style/${collection}/${style}/view">
        <div class="imageSingle">   
          <div class="image"><img src="${Root.getPath()}/${bank}/style/${collection}/${style}/preview"></div>
          <div class="footer">${style}</div>
        </div>
        </a> 
      </#list>
    </div>
    
  </@block>

</@extends>
