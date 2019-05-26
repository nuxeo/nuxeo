function showXMLSources(linkElement, sourcePath, targetElement) {
  if (jQuery(linkElement).parent().hasClass('folded') && !jQuery(targetElement).hasClass('downloaded')) {
    // workaround for IE bug, see NXP-6759
    if (typeof Sarissa != 'undefined') {
      jQuery.ajaxSetup({
        xhr: function() {
          if (Sarissa.originalXMLHttpRequest) {
            return new Sarissa.originalXMLHttpRequest();
          } else if (typeof ActiveXObject != 'undefined') {
            return new ActiveXObject("Microsoft.XMLHTTP");
          } else {
            return new XMLHttpRequest();
          }
        }
      });
    }
    // get sources using Ajax
    jQuery.ajax({
      url: sourcePath,
      dataType: "html",
      cache: false,
      success: function(result) {
        jQuery(targetElement).text(result).css("display", "block");
        jQuery(targetElement).addClass('downloaded');
        prettyPrint();
      },
      error: function(request, status, error) {
        var errorMessage = 'Error retrieving sources for ' + sourcePath;
        if (error != undefined) {
          errorMessage += ': ' + error.description;
        } else {
          errorMessage += '.';
        }
        jQuery(targetElement).text(errorMessage).css("display", "block");
      }
    });
  }
}