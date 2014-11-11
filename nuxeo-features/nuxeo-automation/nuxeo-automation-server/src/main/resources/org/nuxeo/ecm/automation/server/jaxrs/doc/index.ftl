<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <title>Nuxeo Automation Documentation</title>
  </head>
  <style>
  .tree {
    border-left: 1px solid black;
    border-right: 1px solid black;
    border-bottom: 1px solid black;
  }
  .category {
    border-top: 1px solid black;
    border-bottom: 1px solid black;
    padding: 2px;
    background-color: #cafeca
  }
  .category_content {
    padding-top: 10px;
    padding-bottom: 10px;
  }
  .item {
    margin-left: 20px;
  }
  .content {
    margin: 10px 10px;
  }

  .params table {
    border: 1px solid black;
    border-collapse: collapse;
  }

  .params th {
    background-color: #cecece;
  }

  .params td,th {
    border: 1px solid black;
    padding: 6px;
  }


h1 {
/* H1 */
    font: normal small-caps bold 240% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    color: #000;
    border-bottom: 1px solid #333;
    margin: 0;
    margin-bottom: 10px;
}

h2 {
/* H2 */
    background-color: #f1f2f3;
    font: normal normal bold 150% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    color: #111;
    margin: 0px;
    margin: 15px 0px 10px 0px;

}

h3 {
/* H3 */
    background-color: #fff;
    font: normal normal bold 133% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    margin: 10px 0 5px 0;
}

h4 {
    margin: 5px 0 2px 0;
    background-color: #fff;
}

pre {
    background-color: #F1F7FF;
    border: 1px dotted #555555;
    margin: 10px;
    padding: 7px;
    white-space: pre;
    font-size: 0.9em;
}
  </style>
  <body>

  <table width="100%" class="main_table">
  <tr><td colspan="2" align="right"><a href="?">Index</a></td></tr>
  <tr valign="top">
    <td width="30%"> <!-- menu -->
      <div class="tree">
      <#list categories?keys as cat>
        <div class="category">${cat?xml}</div>
        <div class="category_content">
          <#list categories["${cat}"] as item>
          <div class="item"><a href="?id=${item.id}">${item.label}</a></div>
          </#list>
        </div>
      </#list>
      </div>
    </td>
    <td>
    <div class="content">
      <#if operation?has_content>
        <h1>${operation.label}</h1>
        <div class="description">
        ${operation.description}
        </div>
        <h2>General Information</h2>
        <div class="info">
        <div><b>Category:</b> ${operation.category?xml}</div>
        <div><b>Operation Id:</b> ${operation.id}</div>
        </div>
        <h2>Parameters</h2>
        <div class="params">
        <table width="100%">
          <tr align="left">
            <th>Name</th>
            <th>Type</th>
            <th>Required</th>
            <th>Default value</th>
          </tr>
        <#list operation.params as para>
          <tr>
            <td><#if para.isRequired()><b></#if>${para.name}<#if para.isRequired()><b></#if></td>
            <td>${para.type}</td>
            <td><#if para.isRequired()>true<#else>false</#if></td>
            <td>${This.getParamDefaultValue(para)}&nbsp;</td>
          </td>
        </#list>
        </table>
        <h2>Signature</h2>
        </div>
        <div class="signature">
           <div><b>Inputs:</b> ${This.getInputsAsString(operation)}</div>
           <div><b>Outputs:</b> ${This.getOutputsAsString(operation)}</div>
        </div>
        <h2>Links</h2>
        <div><a href="${operation.id}">JSON definition</a></div>
      <#else>
        <h1>Index</h1>
        <#list operations as item>
          <div class="index_item"><a href="?id=${item.id}">${item.label}</a></div>
        </#list>
      </#if>
    </div>
    </td>
  </tr>
  </table>

  </body>
</html>