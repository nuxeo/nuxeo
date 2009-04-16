var proxy = null;
var proxySearch = null;
var lastSelectedCheckBox = null;
var lastSelectAll = null;
var lastTableName = null;

function selectDataTableRow(docRef, providerName, checkbox, listName) {
  if (proxy == null) {
    proxy = Seam.Component.getInstance("documentActions");
  }
  lastSelectedCheckBox = checkbox;
  proxy.processSelectRow(docRef, providerName, listName, checkbox.checked, selectDataTableRowCB);
}

// same as above with current document explicit reference to avoid browser back button to
// trigger errors with the default CURRENT_DOC_CHILDREN provider
function selectDataTableRowForDocument(docRef, providerName, checkbox, listName, currentDocRef) {
  if (proxy == null) {
    proxy = Seam.Component.getInstance("documentActions");
  }
  lastSelectedCheckBox = checkbox;
  proxy.checkCurrentDocAndProcessSelectRow(docRef, providerName, listName, checkbox.checked, currentDocRef, selectDataTableRowCB);
}

// to be deprecated: the search form should use the ResultsProviderCache
// as well
function selectResultsDataTableRow(docRef, selected) {
  if (proxySearch == null) {
    proxySearch = Seam.Component.getInstance("searchActions");
  }
  proxySearch.processSelectRow(docRef, selected, selectDataTableRowCB);
}

function selectDataTablePage(tableId, providerName, selected, listName) {
  if (proxy == null) {
    proxy = Seam.Component.getInstance("documentActions");
  }
  lastSelectAll = selected;
  lastTableName = tableId;
  proxy.processSelectPage(providerName, listName, selected, selectDataTablePageCB);
  handleAllCheckBoxes(tableId, selected);
}

// same as above with current document explicit reference to avoid browser back button to
// trigger errors with the default CURRENT_DOC_CHILDREN provider
function selectDataTablePageForDocument(tableId, providerName, selected, listName, currentDocRef) {
  if (proxy == null) {
    proxy = Seam.Component.getInstance("documentActions");
  }
  lastSelectAll = selected;
  lastTableName = tableId;
  proxy.checkCurrentDocAndProcessSelectPage(providerName, listName, selected, currentDocRef, selectDataTablePageCB);
  handleAllCheckBoxes(tableId, selected);
}

function selectSectionsPage(tableId,selected) {
  //TODO : Not yet used
  /*
      if (proxy == null) {
        proxy = Seam.Component.getInstance("documentActions");
      }
      proxy.processSelectPage(selected, selectDataTableRowCB);
      handleAllCheckBoxes(tableId, selected);
   */
}

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
