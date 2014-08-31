// Script removing the target="JSFFrameId" on forms holding ajaxified calls
var NuxeoFaces = NuxeoFaces || {};
NuxeoFaces.FixFormTargetFrameId = function() {
  var e = {};
  e.apply = function(e) {
    if (typeof e === "undefined") {
      return;
    }
    for ( var n = 0; n < document.forms.length; n++) {
      var o = document.forms[n];
      if (o.target == 'JSFFrameId') {
        o.setAttribute("target", "");
      }
    }
  };
  return e
}();
if (typeof jsf !== "undefined") {
  jsf.ajax.addOnEvent(function(e) {
    if (e.status == "success") {
      NuxeoFaces.FixFormTargetFrameId.apply(e.responseXML)
    }
  })
}
if (typeof jQuery !== "undefined") {
  jQuery(document).ajaxComplete(function(e, t, n) {
    if (typeof t !== "undefined") {
      NuxeoFaces.FixFormTargetFrameId.apply(t.responseXML)
    }
  })
}
