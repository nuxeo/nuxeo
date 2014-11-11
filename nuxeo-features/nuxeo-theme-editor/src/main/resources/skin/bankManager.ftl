

<div class="window">
<div class="title">Manage bank connections</div>
<div class="body">

<#if banks>

<table class="nxthemesManageScreen">
  <tr>
    <th style="width: 20%;">Theme banks</th>
    <th style="width: 80%;">Settings</th>
  </tr>
<tr>
<td>
<ul class="nxthemesSelector">
<#list banks as bank>
  <li <#if selected_bank && bank.name = selected_bank.name>class="selected"</#if>>
    <a href="javascript:NXThemesEditor.selectResourceBank('${bank.name}', 'bank manager')">
    <img src="${basePath}/skin/nxthemes-editor/img/bank-16.png" width="16" height="16" />
    ${bank.name}</a></li>
</#list>
</ul>

</td>
<td>

<#if selected_bank>

  <div style="float: right">
    <img width="200" height="135" src="${selected_bank.connectionUrl}/logo" alt="${selected_bank.name}" />
  </div>

  <form class="nxthemesForm">
  <p><label>Bank name</label>
    <strong>${selected_bank.name}</strong>
  </p>
  <p><label>Connection URL</label>
    <strong>${selected_bank.connectionUrl}</strong>
  </p>
  <p><label>Status</label>
    <#if current_bank && current_bank.name = selected_bank.name>
      <strong style="color: #090;">Connected</strong>&nbsp;
     <a class="nxthemesActionButton" href="javascript:void(0)"
        onclick="NXThemesEditor.useResourceBank('${current_theme.src?js_string}', '', 'bank manager')">
      Disconnect
      </a>
    <#else>
     <strong style="color: #c00">Not connected</strong>&nbsp;
     <button
        class="nxthemesActionButton" href="javascript:void(0)"
        onclick="NXThemesEditor.useResourceBank('${current_theme.src?js_string}', '${selected_bank.name}', 'bank manager')">
      Connect
      </button>
    </#if>
  </p>
  </form>


</#if>

</td>
</tr>
</table>

</div>
</div>

<#if current_bank>

<div class="window">
<div class="title">Bank collections</div>
<div class="body">


<table class="nxthemesManageScreen">
<tr>
<td style="width: 20%">

<form class="nxthemesForm">
<ul class="nxthemesSelector">
<#list collections as collection>
  <#if collection != 'custom'>
    <li <#if selected_bank_collection && selected_bank_collection=collection>class="selected"</#if>><a href="javascript:NXThemesEditor.selectBankCollection('${collection}', 'bank manager')">
     <img src="${basePath}/skin/nxthemes-editor/img/collection-16.png" width="16" height="16" />
      ${collection}</a></li>
  </#if>
</#list>
</ul>
</form>

</td>
<td style="width: 80%">

<#if selected_bank_collection>
  <form action="${current_bank.connectionUrl}/manage/${selected_bank_collection}/download" method="post">
  <p>
    Archive file: <strong>${selected_bank_collection?replace(' ', '-')}.zip</strong>
  </p>
  <p>
    <button class="nxthemesActionButton">Download</button>
  </p>
  </form>
</#if>

</td>
</tr>
</table>

</div>
</div>

</#if>

<#else>
<p>No banks have been registered</p>
</#if>
