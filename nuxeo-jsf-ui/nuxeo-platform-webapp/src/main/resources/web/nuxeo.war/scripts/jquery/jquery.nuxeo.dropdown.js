/*!
 * DropDownMenu management methods
 *
 * @since 5.9.2
 */

(function($) {
  $.fn.dropdown = function() {
    this.click(function(e) {
      var display = $(this).find('ul').css('display');
      if (display === 'none') {
        $('.dropDownMenu').find('ul').hide();
        $(this).find('ul').show();
      } else {
        $(this).find('ul').hide();
      }
      e.stopPropagation();
    });
  };

  $.dropdown = $.fn.dropdown; // Rename for easier calling.
})(jQuery);

// hide all menus on click outside of the menu
jQuery(document).click(function() {
  jQuery('.dropDownMenu').find('ul').hide();
});