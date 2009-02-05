<@extends src="base.ftl">

<@block name="content">
  <h2>More info on document ${Document.title}</h2>
  <h3>Last modified: ${Document["dc:modified"]}</h3>
  <div>
    Document schemas:
    <ul>
    <#list Document.schemas as schema>
      <li> ${schema} </li>
    </#list>
    </ul>
  </div>
  <div>
    Document facets:
    <ul>
    <#list Document.facets as facet>
      <li> ${facet} </li>
    </#list>
    </ul>
  </div>

</@block>

</@extends>
