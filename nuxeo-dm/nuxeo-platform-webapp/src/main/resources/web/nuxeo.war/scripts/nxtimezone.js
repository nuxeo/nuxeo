var nxtz = (function() {

  resetTimeZoneCookieIfNotSet = function() {
    // check the cookie is set
    if (!readCookie("org.jboss.seam.core.TimeZone")) {
      // set tz cookie if not set
      resetTimeZoneCookie();
    }
  };

  readCookie = function(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for ( var i = 0; i < ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) == ' ')
        c = c.substring(1, c.length);
      if (c.indexOf(nameEQ) == 0)
        return c.substring(nameEQ.length, c.length);
    }
    return null;
  }

  resetTimeZoneCookie = function() {
    // retrieves locale timezone thanks to detect_timezone.js
    var timezone = jstz.determine().name();
    // set the expire date in 365 days
    var date = new Date();
    date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
    var expires = "; expires=" + date.toGMTString();
    // reset the cookie using the same one than Seam's TimeZoneSelector
    document.cookie = "org.jboss.seam.core.TimeZone=" + timezone + expires
        + "; path=/";
    // userPreferencesActions.resetTimezone() should
    // be called after in Seam (to reinit the timezone in the Session)
  };
  return {
    resetTimeZoneCookie : resetTimeZoneCookie,
    resetTimeZoneCookieIfNotSet : resetTimeZoneCookieIfNotSet
  };
}());