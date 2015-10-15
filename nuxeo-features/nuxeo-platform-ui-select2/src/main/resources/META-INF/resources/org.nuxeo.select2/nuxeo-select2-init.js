jQuery(document).ready(function() {
  if (window.Prototype) {
    delete Object.prototype.toJSON;
    delete Array.prototype.toJSON;
    delete Hash.prototype.toJSON;
    delete String.prototype.toJSON;
  }
  initSelect2Widgets();
});
jsf.ajax.addOnEvent(function(data) {
  var ajaxstatus = data.status;
  if (ajaxstatus == "success") {
    initSelect2Widgets();
  }
});
nuxeo.utils.addOnEvent(function(data) {
  if (data.status == "success") {
    initSelect2Widgets();
  }
});
