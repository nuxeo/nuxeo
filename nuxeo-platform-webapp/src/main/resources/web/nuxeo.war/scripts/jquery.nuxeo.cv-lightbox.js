(function($) {

  var defaultArgs = {
    type : 'image',
    gallery : {
      enabled : true,
      navigateByImgClick : true,
      preload : [ 0, 1 ],
    },
    closeOnContentClick : true,
    closeBtnInside : false,
    fixedContentPos : true,
    image : {
      verticalFit : true
    },
  };

  $.fn.initNxCv = function(args) {
    jQuery.extend(true, args, defaultArgs);
    jQuery(this).find('div.thumbnailContainer a.image-popup').magnificPopup(
        args);
  }

  $.fn.openLightBoxAfterNP = function() {
    jQuery(this).find('div.thumbnailContainer a.image-popup').magnificPopup(
        'open');
  }

  $.fn.openLightBoxAfterPP = function() {
    var nbItems = jQuery(this).find('div.thumbnailContainer a.image-popup')
        .size();
    jQuery(this).find('div.thumbnailContainer a.image-popup').magnificPopup(
        'open', nbItems - 1);
  }

  $.initNxCv = $.fn.initNxCv;

  $.openLightBoxAfterNP = $.fn.openLightBoxAfterNP;

  $.openLightBoxAfterPP = $.fn.openLightBoxAfterPP;

})(jQuery);