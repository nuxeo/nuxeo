<@extends src="/default/Wiki/base.ftl">
<@block name="content">

<!-- JQuery-needed : actions in tabs, this.title under the tabs and content under this.title -->

<div id="wikipage-actions">
  <ul>
    <li><a href="${this.docURL}@@edit">Edit</a></li>
    <li><a href="${this.docURL}@@view">View</a></li>
    <li><a href="${this.docURL}@@show_versions">History</a></li>
  </ul>
</div>

  <h1>${this.title}</h1>
  
  
  <@transform name="wiki">${this.wikiPage.content}</@transform>
</@block>
</@extends>
