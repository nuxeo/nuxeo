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
            <td class="packageField" style="white-space:pre-line"> ${pkg.description} </td>
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
           <td class="packageLabel">
           <#if pkg.isLocal()==false>
           ${pkg.commentsNumber}
           </#if> comments :</td>
           <td class="packageField">
           <#if pkg.commentsNumber==0 && !pkg.isLocal() >
           &nbsp;
           <#else>
           <span class="commentArea" id="commentArea-${pkg.id}">
           <A href="javascript:fetchComments('${pkg.id}')">get comments...</A>
           </span>
           </#if>
           </td>
          </tr>
          <#if pkg.isLocal()==false>
          <tr>
            <td class="packageLabel">Download count :</td>
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
      <tr>
        <td><span class="boldLabel">Production state</span></td>
        <td> ${Context.getMessage('label.productionState.'+pkg.productionState)}</td>
      </tr><tr>
        <td><span class="boldLabel">Certification status</span></td>
        <td> ${Context.getMessage('label.validationState.'+pkg.validationState)}</td>
      <tr>
        <td><span class="boldLabel">Nuxeo support</span></td>
        <td><#if pkg.isSupported()>
             Yes
          <#else>
             No
          </#if>
        </td>
      </tr>
    </table>
        </span>
      </td>
    </tr>
  </table>

</div>
