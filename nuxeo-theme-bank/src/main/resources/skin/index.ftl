<@extends src="base.ftl">

  <@block name="title">
      Nuxeo Theme Bank
  </@block>

  <@block name="content">
    <h1>Nuxeo Theme Bank</h1>

    <div class="album">
        <#list Root.getBankNames() as bank>
         <a href="${Root.getPath()}/${bank}">
         <div class="imageSingle">   
           <div class="image"><img src="${Root.getPath()}/${bank}/logo" /></div>
           <div class="footer">${bank}</div>
        </div></a>     
        </#list>
    </div>

  </@block>

</@extends>