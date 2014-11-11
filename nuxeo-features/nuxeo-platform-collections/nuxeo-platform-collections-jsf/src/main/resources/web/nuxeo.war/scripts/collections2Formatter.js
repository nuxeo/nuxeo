function formatSuggestedCollection(collection, gravity) {
  var isNew = collection.id && collection.id.indexOf("-999999") == 0;
  if (!gravity) {
    gravity
  }
  var markup = "<table><tbody>";
  if (!isNew) {
    if (gravity) {
      markup = "<table class='tipsyShow' title='" + collection.path + "'><tbody>";
    } else {
      markup = "<table class='tipsyShow " + gravity + "' title='" + collection.path + "'><tbody>";
    }
  } else {
    markup = "<table><tbody>";
  }
  markup += "<tr><td>";
  if (isNew) {
    markup += "<img src='" + window.nxContextPath + "/icons/action_add.gif'/>"
  } else {
    markup += "<img src='" + window.nxContextPath + "/icons/collection.png'/>"
  }
  markup += "</td><td>";
  markup += collection.displayLabel;
  markup += "</td></tr></tbody></table>"
  return markup;
}

function formatSuggestedCollectionW(collection) {
  return formatSuggestedCollection(collection, 'tipsyGravityW');
}

var formatSelectedCollection = formatSuggestedCollection;