(function($) {

  var defaultArgs = {
    type : 'ajax',
    ajax: {
      settings: {
        beforeSend: function (request) {
          nuxeo.lightbox.setRequestHeaders(request);
        }
      }
    },
    callbacks: {
      parseAjax: function(mfpResponse) {
        var jsonDoc = mfpResponse.data;
        mfpResponse.data = nuxeo.lightbox.formatDoc(jsonDoc);
      }
    },
    gallery : {
      enabled : true,
      navigateByImgClick : true,
      preload : [ 0, 1 ],
    },
    closeOnContentClick : false,
    closeBtnInside : false,
    fixedContentPos : true,
    image : {
      verticalFit : true
    }
  };

  $.fn.initNxCv = function(args) {
    jQuery.extend(true, args, defaultArgs);
    jQuery(this).find('a.image-popup').magnificPopup(
        args);
  }

  $.fn.openLightBoxAfterNP = function() {
    jQuery(this).find('a.image-popup').magnificPopup(
        'open');
  }

  $.fn.openLightBoxAfterPP = function() {
    var nbItems = jQuery(this).find('a.image-popup')
        .size();
    jQuery(this).find('a.image-popup').magnificPopup(
        'open', nbItems - 1);
  }

  $.initNxCv = $.fn.initNxCv;

  $.openLightBoxAfterNP = $.fn.openLightBoxAfterNP;

  $.openLightBoxAfterPP = $.fn.openLightBoxAfterPP;

})(jQuery);
