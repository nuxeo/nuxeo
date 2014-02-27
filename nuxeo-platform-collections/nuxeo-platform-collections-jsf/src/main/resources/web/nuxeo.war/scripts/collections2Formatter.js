function formatSuggestedCollection(collection) {
  var markup = "<table><tbody>";
  markup += "<tr><td>";
  if (collection.id && collection.id.indexOf("-999999") == 0) {
    markup += "<img src='" + window.nxContextPath + "/icons/action_add.gif'/>"
  } else {
    markup += "<img src='" + window.nxContextPath + "/icons/collection.png'/>"
  }
  markup += "</td><td>";
  markup += collection.displayLabel;
  markup += "</td></tr></tbody></table>"
  return markup;
}

var formatSelectedCollection = formatSuggestedCollection;