<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>Preset collection: ${collection}
      <a style="float: right" href="${Root.getPath()}/${bank}/${collection}/preset/view">Refresh</a>
    </h1>



  </@block>

</@extends>
