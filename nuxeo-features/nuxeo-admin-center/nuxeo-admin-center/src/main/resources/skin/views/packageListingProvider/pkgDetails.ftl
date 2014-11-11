<div style="pkgDetail">
  <table>
    <tr>
      <td>
        <span class="packagePic">
          <#if pkg.pictureUrl!=null>
           <img src="${pkg.pictureUrl}" alt="Picture Not Available">
          </#if>
          <#if pkg.pictureUrl==null>
           !default image!
          </#if>
        </span>
      </td>
      <td>
        <span class="packageInfo">
    			<table>
     			 <tr>
      		  <td class="packageLabel">Description :</td>
        		<td class="packageField"> ${pkg.description} </td>
     		  </tr>
      		<tr>
       		 <td class="packageLabel">Home Page :</td>
       		 <td class="packageField"> ${pkg.homePage} </td>
     		  </tr>
      		<!--<tr>
        	 <td class="packageLabel">Comments :</td>
        	 <td class="packageField"> to be done ... </td>
      		</tr>-->
      		<tr>
      		  <td class="packageLabel">Download count :</td>
       		  <td class="packageField"> ${pkg.downloadsCount} </td>
      		</tr>
  			</span>
      </td>
    </tr>
  </table>

</div>
