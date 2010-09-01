function showXMLSources(linkElement, sourcePath, targetElement) {
  if (jQuery(linkElement).parent().hasClass('folded') && !jQuery(targetElement).hasClass('downloaded')) {
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
      error: function(result) {
        jQuery(targetElement).text('Error retrieving sources.').css("display", "block");
      }
    });
  }
}