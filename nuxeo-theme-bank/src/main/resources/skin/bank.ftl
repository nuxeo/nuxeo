<@extends src="base.ftl">

  <@block name="title">
      ${bank}
  </@block>

  <@block name="content">
    <h1><span>${bank}</span></h1>
    <p>description ...</p>
    
      <div class="album">
        <a href="${Root.getPath()}/${bank}/style/view">  
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">style</div>
          </div>
        </a>
        <a href="${Root.getPath()}/${bank}/preset/view">  
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">preset</div>
          </div>
        </a>
        <a href="${Root.getPath()}/${bank}/image/view">  
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">image</div>
          </div>
        </a>
        <a href="${Root.getPath()}/${bank}/skins/view">  
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">skins</div>
          </div>
        </a>
    </div>
    
  </@block>

</@extends>
