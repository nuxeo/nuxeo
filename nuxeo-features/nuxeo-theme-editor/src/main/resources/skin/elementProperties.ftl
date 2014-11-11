
<div>

<fieldset class="nxthemesEditor"><legend>Element properties</legend>

<form id="nxthemesElementProperties" class="nxthemesForm" action="" onsubmit="return false">
<#if element_properties>
  <div>
    <input type="hidden" name="id" value="#{selected_element.uid}" />
  </div>
  <#list element_properties as property>
    <p>${property.rendered}</p>
  </#list>
  <div>
    <button type="submit">Update</button>
  </div>
<#else>
  <div>This element has no configurable properties.</div>
</#if>
</form>

</fieldset>
</div>

