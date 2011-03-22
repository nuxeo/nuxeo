jQuery(document).ready(function() {
	jQuery(".category a").click(function(e) {
		function refreshList(name) {
	    var targetUrl = galleryBaseUrl + "/listGadgets";
	    if (name != 'all') {
	        targetUrl += "?cat=" + name;
	    }
	    jQuery.get(targetUrl, function(data) {
	        jQuery('#gadgetListContainer').html(data);
	    });
		};

		e.preventDefault();
    e.stopPropagation();
		jQuery(".currentCategory").toggleClass('currentCategory');
		jQuery(this).toggleClass('currentCategory');
		refreshList(jQuery(this).attr("category-name"));
  });

  jQuery(".specUrl a").click(function(e) {
    e.preventDefault();
    e.stopPropagation();
    parent.window.location = jQuery(this).attr("href");
  });

  if (typeof parent.addGadget == 'function') {
    jQuery(".gadget").click(function(e) {
      var ele = jQuery(this);
      parent.addGadget(ele.attr("gadget-name"), ele.attr("gadget-spec-url"));
    });
    jQuery(".gadget").css("cursor", "pointer");
  }
});
