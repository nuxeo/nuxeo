function formatSuggestedCollection(collection) {
  var isNew = collection.id && collection.id.indexOf("-999999") == 0;
  var markup = "<table><tbody>";
  if (!collection.id) {
    markup = "<table class='select2-nx-disabled'><tbody>";
  } else {
    markup = "<table><tbody>";
  }
  markup += "<tr><td style='width:20px'>";
  if (!isNew) {
    if (collection.icon) {
      markup += "<img class='smallIcon' src='" + window.nxContextPath
          + collection.icon + "'/>"
    }
  } else {
    markup += "<img class='smallIcon' src='" + window.nxContextPath + "/icons/action_add.gif'/>"
  }
  markup += "</td><td>";
  markup += collection.displayLabel;
  markup += "</td></tr></tbody></table>";
  return markup;
}

var formatSelectedCollection = formatSuggestedCollection;