<div style="pkgDetail">

  <span class="packagePic">
    <#if pkg.pictureUrl!=null>
      <img src="${pkg.pictureUrl}" alt="Picture Not Available">
    </#if>
    <#if pkg.pictureUrl==null>
      !default image!
    </#if>
  </span>
  <span class="packageInfo">
    <p><span class="packageLabel">Description:</span><span class="packageField"> ${pkg.description} </span></p>
    <p><span class="packageLabel">Home Page:</span><span class="packageField"> ${pkg.homePage} </span></p>
    <!--<p><span class="packageLabel">Comments:</span><span class="packageField"> to be done ... </span></p>-->
    <p><span class="packageLabel">Download count:</span><span class="packageField"> ${pkg.downloadsCount} </span></p>
  </span>
  <div style="clear:both;"></div>

</div>
