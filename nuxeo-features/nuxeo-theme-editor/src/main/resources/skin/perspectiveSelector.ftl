
<div id="nxthemesPerspectiveSelector">
  <form class="nxthemesPerspectiveSelector" action="javascript:void(0)">
  <label>Perspective:</label>
  <select id="perspective">
    <#list perspectives as perspective>
      <option value="${perspective.name}">${perspective.title}</option>
    </#list>
  </select>
  </form>
</div>
