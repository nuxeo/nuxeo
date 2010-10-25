<@extends src="base.ftl">

  <@block name="title">
      Nuxeo Theme Bank
  </@block>

  <@block name="content">
    <h1>Nuxeo Theme Banks</h1>
    <div class="album">
        <#list Root.getBankNames() as bank>
         <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}')">
         <div class="imageSingle">
           <div class="image"><img src="${Root.getPath()}/${bank}/logo" /></div>
           <div class="footer">${bank}</div>
        </div></a>
        </#list>
    </div>

  </@block>

</@extends>