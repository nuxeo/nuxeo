jQuery.fn.preventDoubleSubmission = function() {
  jQuery(this).on('submit', function(e) {

    var $form = jQuery(this);
    if ($form.data('submitted') === true) {
      // Previously submitted - don't submit again
      e.preventDefault();
      jQuery("#doubleClickMsg").css({
        "display" : "inline"
      });
      window.setTimeout(function() {
        jQuery("#doubleClickMsg").css({
          "display" : "none"
        });
      }, 1500);
    } else {
      // Mark it so that the next submit can be ignored
      $form.data('submitted', true);
    }
  });

  // Keep chainability
  return this;
};