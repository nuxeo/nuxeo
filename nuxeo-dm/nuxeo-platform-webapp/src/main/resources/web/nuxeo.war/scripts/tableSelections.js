var lastSelectedCheckBox = null;
var lastSelectAll = null;
var lastTableName = null;

function handleAllCheckBoxes(tableName, checked) {
  var table = document.getElementById(tableName);
  var listOfInputs = table.getElementsByTagName("input");
  for(var i = 0; i < listOfInputs.length; i++ ){
    if (listOfInputs[i].type == "checkbox"){
      listOfInputs[i].checked = checked;
    }
  }
}

function selectDataTableRowCB(result) {
  if(typeof(result) != "undefined") {
    if (result.indexOf("ERROR") == 0) {
      // should never occur if the application if providers are configured
      // properly
      alert(result);
      if (lastSelectedCheckBox) {
        lastSelectedCheckBox.checked = !lastSelectedCheckBox.checked;
      }
    }
    else {
      var actionsId = result.split("|");
      enableActions(actionsId);
    }
  }
}

function selectDataTablePageCB(result) {
  if(typeof(result) != "undefined") {
    if (result.indexOf("ERROR") == 0) {
      // should never occur if the application if providers are configured
      // properly
      alert(result);
      if (lastSelectAll != null) {
        handleAllCheckBoxes(lastTableName,!lastSelectAll);
      }
    }
    else {
      var actionsIds = result.split("|");
      enableActions(actionsIds);
    }
  }
}

function enableActions(actionsId) {
  var buttonDiv = document.getElementById("selection_buttons");
  if (buttonDiv) {
    var nodes = buttonDiv.childNodes;
    for (var i=0; i<nodes.length; i++) {
      node = nodes[i];
      if (node.tagName == "SPAN") {
        actionId = node.id.split(":")[0];
        enabled = isActionEnabled(actionId, actionsId);
        if (enabled) {
          node.childNodes[0].removeAttribute("disabled");
        }
        else {
          node.childNodes[0].setAttribute("disabled", "disabled");
        }
      }
    }
  }
}

function isActionEnabled(actionId, actionsId) {
  for (var i=0; i<actionsId.length; i++) {
    if (actionsId[i] == actionId) {
      return true;
    }
  }
  return false;
}
