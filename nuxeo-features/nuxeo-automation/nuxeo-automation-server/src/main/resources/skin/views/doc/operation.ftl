<h2>${operation.label}</h2>
<div class="description">
  ${operation.description}
</div>

<h3>General Information</h3>
<div class="info">
  <#if !operation.isChain()>
    <div><span class="sticker sticker-studio">Exposed in Studio</span> <#if operation.addToStudio>Yes</#if><#if !operation.addToStudio>No</#if></div>
  </#if>
  <div><span class="sticker">Category</span> ${operation.category?xml}</div>
  <#if !operation.isChain()>
    <div><span class="sticker">Operation Id</span> ${operation.id}</div>
    <div><span class="sticker">Operation Class</span> ${operation.implementationClass}</div>
  </#if>
  <#if operation.aliases><div><span class="sticker">Operation Aliases</span> - alias:[<#list operation.aliases as alias> ${alias} </#list>]</div></#if>
  <#if operation.since?has_content>
    <div><span class="sticker">Available Since</span> ${operation.since}</div>
  </#if>
  <#if operation.deprecatedSince?has_content>
    <div><span class="sticker sticker-deprecated">Deprecated Since</span> ${operation.deprecatedSince}</div>
  </#if>
</div>

<#if operation.params?has_content>
  <h3>Parameters</h3>
  <div class="params">
    <table width="100%">
      <tr align="left">
        <th>Name</th>
        <th>Description</th>
        <th>Type</th>
        <th>Required</th>
        <th>Default value</th>
      </tr>
      <#list operation.params as para>
      <tr>
        <td><#if para.isRequired()><b></#if>${para.name}<#if para.isRequired()><b></#if></td>
        <td>${para.description}</td>
        <td>${para.type}</td>
        <td><#if para.isRequired()>true<#else>false</#if></td>
        <td>${This.getParamDefaultValue(para)}&nbsp;</td>
      </tr>
      </#list>
    </table>
  </div>
</#if>

<#if operation.isChain() && yaml?has_content>
  <h3>YAML Representation</h3>
  <div>
    <pre>${yaml}</pre>
  </div>
</#if>

<#if This.hasOperation(operation)>
<h3>Operations</h3>
<div class="signature">
  <ul>
    <#list operation.getOperations() as operation>
    <li><a href="${This.path}?id=${operation.getId()}">${operation.getId()}</a></li>
    </#list>
  </ul>
</div>
</#if>

<h3>Signature</h3>
<div class="signature">
  <div><span class="sticker">Inputs</span> ${This.getInputsAsString(operation)}</div>
  <div><span class="sticker">Outputs</span> ${This.getOutputsAsString(operation)}</div>
</div>

<h3>Links</h3>
<div><a href="${Root.path}/${operation.id}">JSON definition</a></div>

<h3>Traces</h3>
<#if This.isTraceEnabled()>
  Traces are enabled: <A href="${This.path}/toggleTraces" class="button">Disable</A><br/>
  <A target="traces" href="${This.path}/traces?opId=${operation.id}">Get traces</A>
<#else>
  Traces are disabled: <A href="${This.path}/toggleTraces" class="button">Enable</A><br/>
  <A target="traces" href="${This.path}/traces?opId=${operation.id}">Get light traces</A>
</#if>
