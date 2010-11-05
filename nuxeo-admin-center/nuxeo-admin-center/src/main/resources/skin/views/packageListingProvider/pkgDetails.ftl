<div style="pkgDetail">
  <table>
    <tr>
      <td>
        <span class="packagePic">
          <#if pkg.pictureUrl!=null>
           <img src="${pkg.pictureUrl}" alt="Picture Not Available">
          </#if>
          <#if pkg.pictureUrl==null>
           <img class="pkgLogo" id="placeholder-${pkg.id}" src="${Root.path}/skin/images/package.png" alt="Loading"/>
           <img class="pkgLogo" id="logo-${pkg.id}" src="${This.getConnectBaseUrl()}marketplace/package/${pkg.id}/logo" onload="showRealLogo('${pkg.id}')" style="display:none">
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
       		 <td class="packageField">
       		 <#if pkg.homePage?? || pkg.homePage=="" >
       		    <A href="${This.getConnectBaseUrl()}marketplace/package/${pkg.id}" target="pkgHomePage"> Home Page on Nuxeo Connect </A>
       		 <#else>
       		    <A href="${pkg.homePage}" target="pkgHomePage"> Home Page </A>
       		 </#if>
       		  </td>
     		</tr>
      		<tr>
        	 <td class="packageLabel"> ${pkg.commentsNumber} comments :</td>
        	 <td class="packageField">
        	 <#if pkg.commentsNumber==0 >
        	 &nbsp;
        	 <#else>
        	 <span class="commentArea" id="commentArea-${pkg.id}">
        	 <A href="javascript:fetchComments('${pkg.id}')">more ...</A>
        	 </span>
        	 </#if>
        	 </td>
      		</tr>
      		<tr>
      		  <td class="packageLabel">Download count :</td>
       		  <td class="packageField"> ${pkg.downloadsCount} </td>
      		</tr>
      	 </table>
  		</span>
      </td>
      <td width="25%">
        <span class="packageInfo">
          <table>
            <tr>
			<td>Target platforms</td>
            <td><#list pkg.getTargetPlatforms() as pf>
                ${pf} &nbsp;
                </#list>
            </td>
            </tr><tr>
        	<td>Package dependencies</td>
        	<td>
	        <#if (pkg.getDependencies()?size==0)>
	            None
	            <#else>
		            <#list pkg.getDependencies() as dep>
		            ${dep.name} &nbsp; (${dep.versionRange.minVersion} -&gt; ${dep.versionRange.maxVersion})<br/>
		            </#list>
		        </#if>
	        </td>
			</tr>
		</table>
        </span>
      </td>
    </tr>
  </table>

</div>
