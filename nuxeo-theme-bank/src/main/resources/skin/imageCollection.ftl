<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <h1>${collection} image</h1>

    <div class="album">
        <#list images as image>
         <div class="imageSingle">   
           <div class="image"><img src="${Root.getPath()}/${bank}/image/${collection}/${image}"></div>
           <div class="footer">${image}</div>
        </div>      
        </#list>
    </div>

  </@block>

</@extends>