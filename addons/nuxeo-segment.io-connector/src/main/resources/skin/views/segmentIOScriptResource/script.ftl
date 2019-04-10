  // SegmentIO SNIPPET
  !function(){var analytics=window.analytics=window.analytics||[];if(!analytics.initialize)if(analytics.invoked)window.console&&console.error&&console.error("Segment snippet included twice.");else{analytics.invoked=!0;analytics.methods=["trackSubmit","trackClick","trackLink","trackForm","pageview","identify","reset","group","track","ready","alias","debug","page","once","off","on"];analytics.factory=function(t){return function(){var e=Array.prototype.slice.call(arguments);e.unshift(t);analytics.push(e);return analytics}};for(var t=0;t<analytics.methods.length;t++){var e=analytics.methods[t];analytics[e]=analytics.factory(e)}analytics.load=function(t){var e=document.createElement("script");e.type="text/javascript";e.async=!0;e.src=("https:"===document.location.protocol?"https://":"http://")+"cdn.segment.com/analytics.js/v1/"+t+"/analytics.min.js";var n=document.getElementsByTagName("script")[0];n.parentNode.insertBefore(e,n)};analytics.SNIPPET_VERSION="4.0.0";
  analytics.load("${writeKey}");
  analytics.page();
  }}();

  function identifyIfNeeded(login, email, traits) {
    var blackList = ${blackListedLogins};
    if (login) {
      if (blackList.indexOf(login)>=0) {
        return;
      }
      // do identify only if the _nxIdentified cookie is not present for this login
      // or there are traits to synchronize
      if (document.cookie.indexOf("_nxIdentified="+login)<0 || traits) {
        // merge email and additional traits
        if(!traits){
        	traits = {};
        }
        traits.email = email;
        // do the identify
        analytics.identify(login, traits);
        document.cookie = "_nxIdentified="+login;
      }
    }
  }
<#if principal??>
  identifyIfNeeded('${principal.name}','${principal.email}');
</#if>
