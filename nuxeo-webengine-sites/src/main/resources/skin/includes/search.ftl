<#macro search>

<div class="logo" >
  <div style="vertical-align: right;">
    <table >
      <tr style="vertical-align: top;">
        <td style="padding-right: 10px;text-align: right;>
          <div id="searchPage">
           <form id="serach" action="${This.path}/search" method="POST" accept-charset="utf-8">  
           	<input type="text" name="searchParam"  size="20"/>
           	<br/>
           	<input type="submit" name="search_page" value="Search" id="search_page" >
           </form>
		</div>
        </td>
      </tr>
    </table>
  </div>
</div>

</#macro>