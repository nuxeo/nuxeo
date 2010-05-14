<@extends src="base.ftl">

  <@block name="title">
      <title>${resource}</title>
  </@block>

  <@block name="content">
    <h1>${resource}</h1>
    <img src="${Root.getPath()}/${path}" />
  </@block>

</@extends>
