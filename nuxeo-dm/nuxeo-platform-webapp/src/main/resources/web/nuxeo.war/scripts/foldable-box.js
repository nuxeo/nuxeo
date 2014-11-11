function toggleBox(toggleButton) {
  var title = toggleButton.parentNode;
  var body;
  if (title.nextSiblings)
    body = title.nextSiblings()[0];
  else
    body = title.parentNode.children[1];
  if (Element.hasClassName(title, 'folded')) {
    Element.removeClassName(title, 'folded');
    Element.addClassName(title, 'unfolded');
  } else {
    Element.removeClassName(title, 'unfolded');
    Element.addClassName(title, 'folded');
  }
  Effect.toggle(body, 'blind', {duration:0.2});
  return false;
}

function toggleBoxFor(title, body) {
  if (Element.hasClassName(title, 'folded')) {
    Element.removeClassName(title, 'folded');
    Element.addClassName(title, 'unfolded');
  } else {
    Element.removeClassName(title, 'unfolded');
    Element.addClassName(title, 'folded');
  }
  Effect.toggle(body, 'blind', {duration:0.2});
  return false;
}