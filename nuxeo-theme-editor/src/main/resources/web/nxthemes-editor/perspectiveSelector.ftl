<#assign perspectives = script("getPerspectives.groovy") />

<div id="nxthemesPerspectiveSelector">

  <form class="nxthemesPerspectiveSelector" action="javascript:void(0)"
            submit="return false">

  <label>Perspective:</label>

  <select id="perspective">

  <#list perspectives as perspective>
  <option value="${perspective.name}">${perspective.title}</option>
  </#list>

  </select>

  <button type="submit">OK</button>

  </form>
</div>
