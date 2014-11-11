function toggleBox(toggleButton) {
  var title = toggleButton.parentNode;
  var body;
  if (title.nextSiblings)
    body = title.nextSiblings()[0];
  else
    body = title.parentNode.children[1];
  if (Element.hasClassName(title, 'folded')) {
    title.className = 'unfolded';
  } else {
    title.className = 'folded';
  }
  Effect.toggle(body, 'blind', {duration:0.2});
  return false;
}
