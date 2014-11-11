var popupStatus = 0;

function loadPopup() {
  // loads popup only if it is disabled
  if (popupStatus == 0) {
    jqw("#backgroundPopup").css( {
      "opacity" : "0.7"
    });
    jqw("#backgroundPopup").fadeIn("slow");
    jqw("#popupChooser").fadeIn("slow");
    popupStatus = 1;
  }
}

// disabling popup with jQuery magic!
function disablePopup() {
  // disables popup only if it is enabled
  if (popupStatus == 1) {
    jqw("#backgroundPopup").fadeOut("slow");
    jqw("#popupChooser").fadeOut("slow");
    popupStatus = 0;
  }
}

function centerPopup() {
  var windowWidth = document.documentElement.clientWidth;
  var windowHeight = document.documentElement.clientHeight;
  var popupHeight = jqw("#popupChooser").height();
  var popupWidth = jqw("#popupChooser").width();
  var topPos = windowHeight / 2 - popupHeight / 2;
  if (topPos<0) {
    topPos=100;
  }
  jqw("#popupChooser").css( {
    "position" : "absolute",
    "top" : topPos,
    "left" : windowWidth / 2 - popupWidth / 2
  });
  jqw("#backgroundPopup").css( {
    "height" : windowHeight
  });

}

function showPopup(targetUrl) {
  centerPopup();
  loadPopup();

  jqw.get(targetUrl, function(data) {
    jqw('#popupContent').html(data);
  });
  // Click out event!
  jqw("#backgroundPopup").click(function() {
    disablePopup();
  });
  // Press Escape event!
  jqw(document).keypress(function(e) {
    if (e.keyCode == 27 && popupStatus == 1) {
      disablePopup();
    }
  });
  jqw("#popupChooserClose").click(function() {
    disablePopup();
  });

}