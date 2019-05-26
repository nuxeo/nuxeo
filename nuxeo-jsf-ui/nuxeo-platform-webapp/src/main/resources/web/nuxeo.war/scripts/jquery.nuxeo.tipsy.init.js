jQuery(document).ready(function() {
  jQuery('.tipsyShow').initTipsy(500, 5000);
});
jsf.ajax.addOnEvent(function(data) {
  var ajaxstatus = data.status;
  if (ajaxstatus == "success") {
    // remove all existing tipsy
    jQuery('.tipsy').remove();
    jQuery('.tipsyShow').initTipsy(500, 5000);
  }
});