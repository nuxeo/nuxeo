(function($) {

  $.fn.konami = function(imagePath) {
    var kkeys = [], konami = "38,38,40,40,37,39,37,39,66,65";
    $(this).bind("keydown", function(e) {
      kkeys.push(e.keyCode);
      if (kkeys.toString().indexOf(konami) >= 0) {
        $(this).unbind(e);
        var divContent = '<div id="konamiContainer" style="overflow: hidden; position: absolute; left: 0px; top: '
          + ($(window).height() + $(window).scrollTop() - 102) + 'px; width: 100%;"><img src="'
          + imagePath + '" id="konami" alt="" width="102"'
          + ' style="position: relative; left: -102px; bottom: 0px;" /></div>';
        $('body').append(divContent);
        $('#konami').animate({
          left: '+=' + ($(window).width() + 61)},
          15000,
          'linear',
          function() { $('#konamiContainer').remove(); });
      }
    }, true);
  }

})(jQuery);