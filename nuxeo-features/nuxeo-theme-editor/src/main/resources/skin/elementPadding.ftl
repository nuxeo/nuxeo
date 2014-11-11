
<div id="nxthemesPaddingEditor" class="nxthemesToolbox">

<div class="title">
<img class="close" onclick="javascript:NXThemes.getControllerById('editor perspectives').switchTo('canvas editor')"
     src="${basePath}/skin/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />
     Padding editor</div>

<#if selected_element>

<form class="nxthemesForm" action="" onsubmit="return false">
  <table cellpadding="0" cellspacing="0" style="width: 100%">
      <tr>
        <td></td>
        <td><input type="text" size="6" name="padding-top" tabindex="1"
            value="${padding_of_selected_element.top}" />
        </td>
        <td></td>
      </tr>
      <tr>
        <td><input type="text" size="6" name="padding-left" tabindex="4"
            value="${padding_of_selected_element.left}" />
        </td><td>
          <div class="nxthemesFragment" style="height: 50px;" />
        </td>
        <td style="text-align: center">
          <input type="text" size="6" name="padding-right" tabindex="2"
            value="${padding_of_selected_element.right}" />
        </td>
      </tr>
      <tr>
        <td></td>
        <td><input type="text" size="6" name="padding-bottom" tabindex="3"
            value="${padding_of_selected_element.bottom}" />
        </td>
        <td></td>
      </tr>
      <tr>
        <td colspan="3">
          <button type="submit">Update</button>
          <button type="submit" onclick="NXThemes.getControllerById('editor perspectives').switchTo('canvas editor');">Close</button>
        </td>
      </tr>
    </table>

</form>

<#else>
   <p class="nxthemesEditor" style="text-align: center"><em>No element is selected.</em></p>
</#if>

</div>

