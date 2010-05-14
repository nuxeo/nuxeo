<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <h1>${collection} image</h1>

    <div class="album">
        <#list images as image>
         <div class="imageSingle">   
           <div class="image"><img src="${Root.getPath()}/${bank}/image/${collection}/${image}"
            style="border: 1px inset #999; max-width: 100px; max-height: 100px" /></div>
           <div class="footer">${image}</div>
        </div>      
        </#list>
    </div>

  </@block>

</@extends>