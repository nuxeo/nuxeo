(function() {
  var cookieName = "nuxeo.adminMesage.cookie";
  jQuery(document).ready(function() {
    jQuery(".adminMessage").each(function() {
      var adminMessageEle = jQuery(this);
      adminMessageEle.find(".close").click(function() {
        adminMessageEle.addClass("displayN");
        jQuery.cookie(cookieName, adminMessageEle.data('timestamp'));
      });

      var timestamp = parseInt(jQuery.cookie(cookieName));
      if (!timestamp) {
        adminMessageEle.removeClass("displayN");
      } else if (timestamp !== adminMessageEle.data("timestamp")) {
        // admin message modified
        adminMessageEle.removeClass("displayN");
      }
    });
  });
})();
