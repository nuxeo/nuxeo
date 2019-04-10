<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<h1> view Contribution ${contribution.id} </h1>

<div class="tabscontent">

  <pre>
    <code>
    ${contribution.xml?html}
    </code>
  </pre>

  <h2> target ExtensionPoint </h2>
  <a href="${Root.path}/${distId}/viewExtensionPoint/${contribution.extensionPoint}">
  ${contribution.extensionPoint}
  </a>

</div>
</@block>

</@extends>