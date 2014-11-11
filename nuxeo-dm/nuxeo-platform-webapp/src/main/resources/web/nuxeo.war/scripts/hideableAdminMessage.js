
jQuery(function() {
 jQuery('.header_admin_messages .actionHide').each(function() {
  if (jQuery.cookie(buildCookieName(jQuery(this)))) { jQuery(this).remove(); return; }

  var hide = jQuery('<a><img src="icons/delete.png" alt="hide" /></a>').attr({href: '#'}).click(function() {
    var parent = jQuery(this).parent();
    parent.fadeOut();
    jQuery.cookie(buildCookieName(parent), true);
    return false;
  });
  hide.addClass('hideBtn')
  jQuery(this).append(hide)
})
});

function buildCookieName(elt) {
  return 'nuxeo.' + elt.attr('id') + '.cookie';
}
