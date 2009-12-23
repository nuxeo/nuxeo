<div>
  
  <table>
  <tr>
  <td>
  ${Document.webcontainer.welcomeText}
  </td>
  </tr>
  
  <tr>
  <td>
	<#if This.welcomeMediaIsImage == -1>
	<#elseif This.welcomeMediaIsImage == 0>
	  <img src="${This.path}/welcomeMedia" alt="logo" style="width: ${This.welcomeMediaWidth}px; height: ${This.welcomeMediaHeight}px"/>
	<#elseif This.welcomeMediaIsImage == 1>
	  <object width="${This.welcomeMediaWidth}" height="${This.welcomeMediaHeight}">
		<param name="movie" value="${This.path}/welcomeMedia">
			<embed src="${This.path}/welcomeMedia" width="${This.welcomeMediaWidth}" height="${This.welcomeMediaHeight}">
		</embed>
	  </object>
	</#if>  
 </td>
 </tr>
 </table>
</div>
