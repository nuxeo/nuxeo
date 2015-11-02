function toggleBox(toggleButton) {
  var title = toggleButton.parentNode;
  var body;
  if (title.nextSiblings) {
    body = title.nextSiblings()[0];
  } else {
    body = title.parentNode.children[1];
  }

  var element = jQuery(title);
  if (element.hasClass('folded')) {
    element.removeClass('folded');
    element.addClass('unfolded');
  } else {
    element.removeClass('unfolded');
    element.addClass('folded');
  }

  jQuery(body).slideToggle('fast');
  return false;
}

function toggleBoxFor(title, body) {
  var element = jQuery(title);
  if (element.hasClass('folded')) {
    element.removeClass('folded');
    element.addClass('unfolded');
  } else {
    element.removeClass('unfolded');
    element.addClass('folded');
  }

  jQuery(body).slideToggle('fast');
  return false;
}
