var nuxeo = nuxeo || {};

nuxeo.mobile = (function(m) {

  function isAndroid() {
    return /Android/.test(window.navigator.userAgent);
  }

  function isIOS() {
    return /iPhone|iPad|iPod/.test(window.navigator.userAgent);
  }

  m.displayMobileBanner = function(bannerId, displayStyle, androidLinkId, iOSLinkId) {
    if (isAndroid()) {
      document.getElementById(bannerId).style.display = displayStyle;
      document.getElementById(iOSLinkId).style.display = 'none';
    }
    if (isIOS()) {
      document.getElementById(bannerId).style.display = displayStyle;
      document.getElementById(androidLinkId).style.display = 'none';
    }
  }

  var openIOSAppTimer;
  var openAppStoreTimer;

  function clearTimers() {
    clearInterval(openIOSAppTimer);
    clearInterval(openAppStoreTimer);
  }

  m.openIOSAppOrAppStore = function(appURL, storeURL) {
    window.location = appURL;
    var click = Date.now();
    openIOSAppTimer = setInterval(function() {
      if (document.hidden || document.webkitHidden) {
        clearTimers();
      }
    }, 200);
    openAppStoreTimer = setInterval(function() {
      if (!document.hidden && !document.webkitHidden && Date.now() - click > 2000) {
        clearTimers();
        window.location = storeURL;
      }
    }, 200);
  }

  return m;
}(nuxeo.mobile || {}));
