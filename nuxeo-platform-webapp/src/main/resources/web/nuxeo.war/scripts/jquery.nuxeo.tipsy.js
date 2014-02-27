(function($) {

  var check = "tipsyGravity";

  $.fn.initTipsy = function() {

    var cls = jQuery(this).attr('class').split(' ');
    var gravity = jQuery.fn.tipsy.autoNS;
    for (var i = 0; i < cls.length; i++) {
      if (cls[i].indexOf(check) > -1) {
        gravity = cls[i].slice(check.length, cls[i].length).toLowerCase();
      }
    }

    jQuery(this).tipsy({
      title : 'title',
      gravity : gravity
    });

    return this;
  };

  $.initTipsy = $.fn.initTipsy; // Rename for easier calling.

})(jQuery);
