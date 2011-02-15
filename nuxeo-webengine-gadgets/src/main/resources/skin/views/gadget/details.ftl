<div>
<span class="gadgetTitle">${gadget.getTitle()}</span>
<div class="addButton"><input type="button" value="add" onclick="doAddGadget('${gadget.name}','${gadget.getGadgetDefinition().toString()}')"/></div>
<table>
  <tr>
      <td colspan="2">
          <#if gadget.getScreenshot()>
            <#if gadget.getScreenshot()?starts_with("http")>
                <img src="${gadget.getScreenshot()}" width="280px"/>
            <#else>
                <img src="${Context.baseURL}${gadget.getScreenshot()}" width="280px"/>
            </#if>
          <#else>
            <img src="${skinPath}/img/default-screenshot.jpg"/>
          </#if>
      </td>
  </tr>
  <tr>
    <td>Name :</td>
    <td>${gadget.name}</td>
  </tr>
  <tr>
    <td>Author :</td>
    <td>${gadget.author}</td>
  </tr>
  <tr>
    <td colspan="2"><A target="gadgetSpec" href="${gadget.getGadgetDefinition().toString()}">Gadget Spec URL</A></td>
  </tr>
  <tr>
    <td colspan="2">
    Required features :
    <ul>
      <#assign features=gadget.getGadgetSpec().getModulePrefs().getFeatures()/>
      <#list features?keys as featureName>
      <li>${featureName}
      <#if !features[featureName].getRequired() >
       (optional)
      </#if>
      </li>
      </#list>
    </ul>
    </td>
  </tr>

</table>
</div>