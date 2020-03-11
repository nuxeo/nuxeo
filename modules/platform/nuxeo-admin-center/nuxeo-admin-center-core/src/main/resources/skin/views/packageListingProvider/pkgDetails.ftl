<div class="pkgDetail">
  <table>
    <tr>
      <td>
        <span class="packagePic">
          <#if pkg.pictureUrl!=null>
           <img src="${pkg.pictureUrl}" alt="Picture Not Available">
          </#if>
          <#if pkg.pictureUrl==null>
           <img class="pkgLogo" width="95px" id="placeholder-${pkg.id}" src="${Root.path}/skin/images/package.png" alt="Loading"/>
           <img class="pkgLogo" width="95px" id="logo-${pkg.id}" src="${This.getConnectBaseUrl()}marketplace/package/${pkg.id}/logo" onload="showRealLogo('${pkg.id}')" style="display:none">
          </#if>
        </span>
      </td>
      <td>
        <span class="packageInfo">
         <table>
         <tr>
            <td class="packageLabel">${Context.getMessage('label.pkgDetails.titles.description')}</td>
            <td class="packageField" style="white-space:pre-line"> ${pkg.description} </td>
         </tr>
         <tr>
            <td class="packageLabel">${Context.getMessage('label.pkgDetails.titles.home')}</td>
            <td class="packageField">
            <#if pkg.homePage?? || pkg.homePage=="" >
               <A href="${This.getConnectBaseUrl()}marketplace/package/${pkg.id}" target="pkgHomePage">${Context.getMessage('label.pkgDetails.links.home.connect')}</A>
            <#else>
               <A href="${pkg.homePage}" target="pkgHomePage">${Context.getMessage('label.pkgDetails.links.home')}</A>
            </#if>
            </td>
         </tr>
         <#if !pkg.isLocal()>
         <tr>
           <td class="packageLabel">
            ${pkg.commentsNumber}&nbsp;${Context.getMessage('label.pkgDetails.titles.comments')}
           </td>
           <td class="packageField">
             <#if pkg.commentsNumber==0 >
               &nbsp;
             <#else>
             <span class="commentArea" id="commentArea-${pkg.id}">
               <a href="javascript:fetchComments('${pkg.id}')">${Context.getMessage('label.pkgDetails.links.comments')}</a>
             </span>
             </#if>
           </td>
          </tr>
          <tr>
            <td class="packageLabel">${Context.getMessage('label.pkgDetails.titles.downloadcount')}</td>
             <td class="packageField"> ${pkg.downloadsCount} </td>
          </tr>
          </#if>
         </table>
      </span>
      </td>
      <td width="25%">
        <span class="packageInfo">
          <table>
            <tr>
	    <td><span class="boldLabel">${Context.getMessage('label.pkgDetails.titles.target')}</span></td>
            <td class="packageField"><#list pkg.getTargetPlatforms() as pf>
                ${pf} &nbsp;
                </#list>
            </td>
            </tr><tr>
          <td><span class="boldLabel">${Context.getMessage('label.pkgDetails.titles.package.dependencies')}</span></td>
          <td class="packageField">
          <#if (pkg.getDependencies()?size==0)>
              ${Context.getMessage('label.pkgDetails.messages.package.dependencies.none')}
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
