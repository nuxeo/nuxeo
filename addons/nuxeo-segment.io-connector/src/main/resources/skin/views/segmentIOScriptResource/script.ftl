  // integrate Segment IO
  window.analytics||(window.analytics=[]),window.analytics.methods=["identify","track","trackLink","trackForm","trackClick","trackSubmit","page","pageview","ab","alias","ready","group","on","once","off"],window.analytics.factory=function(t){return function(){var a=Array.prototype.slice.call(arguments);return a.unshift(t),window.analytics.push(a),window.analytics}};for(var i=0;i < window.analytics.methods.length;i++){var method=window.analytics.methods[i];window.analytics[method]=window.analytics.factory(method)}window.analytics.load=function(t){var a=document.createElement("script");a.type="text/javascript",a.async=!0,a.src=("https:"===document.location.protocol?"https://":"http://")+"d2dq2ahtl5zl1z.cloudfront.net/analytics.js/v1/"+t+"/analytics.min.js";var n=document.getElementsByTagName("script")[0];n.parentNode.insertBefore(a,n)},window.analytics.SNIPPET_VERSION="2.0.8",
  window.analytics.load("${writeKey}");
  window.analytics.page();

  function marketoUpdate(email, key) {
    if (Munchkin) {
       Munchkin.munchkinFunction('associateLead',{Email: email}, key);
    }
  }

  function identifyIfNeeded(login, email) {
    var blackList = ${blackListedLogins};
    if (login) {
      if (blackList.indexOf(login)>=0) {
        return;
      }
      if (document.cookie.indexOf("_nxIdentified="+login)<0) {
        // do the identify
         analytics.identify(login, {
          email   : email
         }, ${providers});
         if (jQuery) {
            // forward to Marketo
            jQuery.get('${This.path}/marketo/' + email).done(function(key){
              marketoUpdate(email,key);
            });
         }
         document.cookie = "_nxIdentified="+login;
      }
    }
   }
<#if principal??>
   identifyIfNeeded('${principal.name}','${principal.email}');
</#if>
