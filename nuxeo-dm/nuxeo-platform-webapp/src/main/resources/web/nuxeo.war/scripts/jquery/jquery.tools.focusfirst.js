(function($) {

  $.fn.focusFirst = function() {
    var elem = $('input:visible', this).get(0);
    var select = $('select:visible', this).get(0);
    if (select && elem) {
      if (select.offsetTop < elem.offsetTop) {
        elem = select;
      }
    }

    var textarea = $('textarea:visible', this).get(0);
    if (textarea && elem) {
      if (textarea.offsetTop < elem.offsetTop) {
        elem = textarea;
      }
    } 
  
    if (elem) {
      try {
        elem.focus();
      } catch(err) { }
    }
    return this;
  };

})(jQuery);