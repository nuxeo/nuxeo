jQuery(document).ready(function() {
  function addClickListeners() {
    jQuery(".specUrl a").click(function(e) {
      e.preventDefault();
      e.stopPropagation();
      parent.window.location = jQuery(this).attr("href");
    });

    if (typeof parent.nuxeo.openSocial.addGadget== 'function') {
      jQuery(".gadget").click(function() {
        var ele = jQuery(this);
        parent.nuxeo.openSocial.addGadget(ele.attr("gadget-name"), ele.attr("gadget-spec-url"));
      });
      jQuery(".gadget").css("cursor", "pointer");
    }
  }

	jQuery(".category a").click(function(e) {
		function refreshList(name) {
	    var targetUrl = galleryBaseUrl + "/listGadgets";
      if (typeof language !== 'undefined') {
        targetUrl += "?" + language;
      }
	    if (name != 'all') {
	        targetUrl += "&cat=" + name;
	    }
	    jQuery.get(targetUrl, function(data) {
	        jQuery('#gadgetListContainer').html(data);
          addClickListeners();
	    });
		}

		e.preventDefault();
    e.stopPropagation();
		jQuery(".currentCategory").toggleClass('currentCategory');
		jQuery(this).toggleClass('currentCategory');
		refreshList(jQuery(this).attr("category-name"));
  });

  addClickListeners();
});
