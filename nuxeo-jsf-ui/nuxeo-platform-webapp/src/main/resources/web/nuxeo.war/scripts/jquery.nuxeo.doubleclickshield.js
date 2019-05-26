(function($) {
  $.fn.preventDoubleSubmission = function() {

    var $form = jQuery(this);
    // Did we initialize dc shield on this form yet?
    if ($form.data('dc_shielded') !== true) {

      $form.data('dc_shielded', true);

      $form.on('submit', function(e) {

        if ($form.data('submitted') === true) {
          // Previously submitted - don't submit again
          e.preventDefault();
          jQuery.ambiance({
            title : nuxeo.doubleClickShield.message,
            className : "infoFeedback",
            timeout : 1.5
          });
        } else {
          // Mark it so that the next submit can be ignored
          $form.data('submitted', true);
        }
      });
    }

    // Keep chainability
    return this;
  };

  $.preventDoubleSubmission = $.fn.preventDoubleSubmission; // Rename for easier calling.

})(jQuery);
