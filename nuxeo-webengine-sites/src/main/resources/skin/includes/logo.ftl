<#macro logo>

<div style="width: 100%;height: 30%;background-color: black;text-align: left;padding-top: 30px;padding-left: 30px;padding-bottom: 30px" >
  <div style="vertical-align: middle;">
    <table >
      <tr style="vertical-align: top;">
        <td>
          <img style="width: 70px;height: 70px;" src="${This.path}/logo" alt="logo">
        </td>
        <td style="padding-left: 10px;text-align: left;">
          <div style="font-size: large;font-style: italic; color: white;">
            ${siteName}
          </div>
          <div style="font-size: medium;font-style: italic; color: white;">
            ${description} 
          </div>
        </td>
      </tr>
    </table>
  </div>
</div>

</#macro>