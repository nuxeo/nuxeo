jQuery.noConflict();

function togglePanel(button) {
  button = jQuery(button);
  var parent = button.parent();
  while (parent != null && !parent.hasClass('togglePanel')) {
    parent = parent.parent();
  }
  var ele = jQuery(parent.find('.togglePanelBody')[0]);
  ele.toggle();
  button.toggleClass('folded').toggleClass('unfolded');
  return false;
}
