var popupStatus = 0;
var cssLoaded=0;

function loadPopup() {
  // loads popup only if it is disabled
  if (popupStatus == 0) {
    jQuery("#backgroundPopup").css( {
      "opacity" : "0.7"
    });
    jQuery("#backgroundPopup").fadeIn("slow");
    jQuery("#popupChooser").fadeIn("slow");
    popupStatus = 1;
  }
}

// disabling popup with jQuery magic!
function disablePopup() {
  // disables popup only if it is enabled
  if (popupStatus == 1) {
    jQuery("#backgroundPopup").fadeOut("slow");
    jQuery("#popupChooser").fadeOut("slow");
    popupStatus = 0;
  }
}

function centerPopup() {
  var windowWidth = document.documentElement.clientWidth;
  var windowHeight = document.documentElement.clientHeight;
  var popupHeight = jQuery("#popupChooser").height();
  var popupWidth = jQuery("#popupChooser").width();
  var topPos = windowHeight / 2 - popupHeight / 2;
  if (topPos<0) {
    topPos=100;
  }
  jQuery("#popupChooser").css( {
    "position" : "absolute",
    "top" : topPos,
    "left" : windowWidth / 2 - popupWidth / 2
  });
  jQuery("#backgroundPopup").css( {
    "height" : windowHeight
  });
}


function appendCSSData(cssData) {
  if (jQuery("head").children("style").length==0) {
  // add the style tag
    jQuery("head").append("<style>" + cssData + "</style>");
  } else {
  // append style content
    jQuery("head").children("style").append(cssData);
  }
}

function showPopup(targetUrl) {
  if (cssLoaded==0) {
      // load the css stuff
      jQuery.get("/nuxeo/site/skin/gadgets/css/gadget-popup-style.css", function(data) {appendCSSData(data); doShowPopup(targetUrl);});
      cssLoaded=1;
  }
  else {
    doShowPopup(targetUrl);
  }
}

function doShowPopup(targetUrl) {
  centerPopup();
  loadPopup();
  jQuery.get(targetUrl, function(data) {
    jQuery('#popupContent').html(data);
  });
  // Click out event!
  jQuery("#backgroundPopup").click(function() {
    disablePopup();
  });
  // Press Escape event!
  jQuery(document).keypress(function(e) {
    if (e.keyCode == 27 && popupStatus == 1) {
      disablePopup();
    }
  });
  jQuery("#popupChooserClose").click(function() {
    disablePopup();
  });

}

function addGadgetHook(name,url) {
  disablePopup();
}
