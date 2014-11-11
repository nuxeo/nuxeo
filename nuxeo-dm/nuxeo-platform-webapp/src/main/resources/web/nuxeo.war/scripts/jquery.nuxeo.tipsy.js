(function($) {

  var check = "tipsyGravity";

  $.fn.initTipsy = function(delayIn, resetDelayInTimeout) {

    // default 5s
    resetDelayInTimeout = typeof resetDelayInTimeout !== 'undefined' ? resetDelayInTimeout
        : 5000;

    var $targetElts = $(this);

    $targetElts.doInit(delayIn);

    $(document).on("tipsy-show", function() {
      reduceDelay(delayIn, resetDelayInTimeout, $targetElts);
    });

    return this;
  };

  reduceDelay = function(delayIn, resetDelayInTimeout, $targetElts) {
    $(document).unbind("tipsy-show");
    $targetElts.each(function() {
      $(this).data('tipsy-delayin', 0);
    });
    var timer = scheduleResetDelay(delayIn, resetDelayInTimeout, $targetElts);
    $(document).on("tipsy-show", function() {
      clearTimeout(timer);
      timer = scheduleResetDelay(delayIn, resetDelayInTimeout, $targetElts);
    });
  }

  scheduleResetDelay = function(delayIn, resetDelayInTimeout, $targetElts) {
    return window.setTimeout(function() {
      $(document).unbind("tipsy-show");
      $targetElts.each(function() {
        $(this).data('tipsy-delayin', delayIn);
      });
      $(document).on("tipsy-show", function() {
        reduceDelay(delayIn, resetDelayInTimeout, $targetElts);
      });
    }, resetDelayInTimeout);
  }

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
