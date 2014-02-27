(function($) {

  var check = "tipsyGravity";

  $.fn.initTipsy = function(delayIn) {

    $(this).doInit(delayIn);

    $(document).on("tipsy-show", function() {
      $(document).unbind("tipsy-show");
      $('.tipsyShow').each(function() {
        $(this).doInit(0);
      });
    });

    return this;
  };

  $.fn.doInit = function(delayIn) {

    // default 500ms
    delayIn = typeof delayIn !== 'undefined' ? delayIn : 500;

    $(this).each(function() {
      var cls = $(this).attr('class').split(' ');
      var gravity = $.fn.tipsy.auto;
      for ( var i = 0; i < cls.length; i++) {
        if (cls[i].indexOf(check) > -1) {
          gravity = cls[i].slice(check.length, cls[i].length).toLowerCase();
        }
      }

      $(this).tipsy({
        title : 'title',
        gravity : gravity,
        live : true,
        delayIn : delayIn
      });
    });
  }

  $.initTipsy = $.fn.initTipsy; // Rename for easier calling.

})(jQuery);
