jQuery.noConflict();

function togglePanel(ele) {
  var parent = jQuery(ele).parent();
  while (parent != null && !parent.hasClass('togglePanel')) {
    parent = parent.parent();
  }
  var ele = jQuery(parent.find('.togglePanelBody')[0]);
  ele.toggle("normal");
  return false;
}
