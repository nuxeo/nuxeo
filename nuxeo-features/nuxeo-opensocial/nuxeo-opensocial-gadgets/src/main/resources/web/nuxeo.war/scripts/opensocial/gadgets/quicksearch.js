var prefs = new gadgets.Prefs();

// configure Automation REST call
var NXRequestParams = { operationId:'Document.PageProvider',
  operationParams:{
    pageSize:10,
    documentLinkBuilder: prefs.getString("documentLinkBuilder")
  },
  operationContext:{}, // context
  operationDocumentProperties:"common,dublincore", // schema that must be fetched from resulting documents
  entityType:'documents', // result type : only document is supported for now
  usePagination:true, // manage pagination or not
  displayMethod:displayDocumentList, // js method used to display the result
  noEntryLabel: prefs.getMsg('label.gadget.no.document')
};

function doSearch() {
  var txt = _gel('searchPattern').value;
  runSearch(txt);
}

function runSearch(txt) {
  var queryType = prefs.getString("queryType");
  var displayMode = prefs.getString("displayMode");
  if ('NXQL' == queryType) {
    NXRequestParams.operationParams.query = txt;
    delete NXRequestParams.operationParams.queryParams;
  } else {
    NXRequestParams.operationParams.query = "SELECT * FROM Document WHERE ecm:fulltext LIKE ? AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";
    NXRequestParams.operationParams.queryParams = txt;
  }
  if ('COMPACT' == displayMode) {
    NXRequestParams.displayColumns = [
      { type:'builtin', field:'icon'},
      {type: 'builtin', field: 'titleWithLink', label: prefs.getMsg('label.dublincore.title')}
    ];
  } else {
    NXRequestParams.displayColumns = [
      { type:'builtin', field:'icon'},
      {type: 'builtin', field: 'titleWithLink', label: prefs.getMsg('label.dublincore.title')},
      {type: 'date', field: 'dc:modified', label: prefs.getMsg('label.dublincore.modified')},
      {type: 'text', field: 'dc:creator', label: prefs.getMsg('label.dublincore.creator')}
    ];
  }
  doAutomationRequest(NXRequestParams);
}

function doSaveSearch() {
  var txt = _gel('searchPattern').value;
  prefs.set('savedQuery', txt);
  _gel("searchBox").style.display = 'none';
  _gel("titleBox").style.display = 'block';
  _gel("queryText").innerHTML = txt;
  runSearch(txt);
}

function loadSearch() {
  var txt = gadgets.util.unescapeString(prefs.getString('savedQuery'));
  if (txt) {
    runSearch(txt);
    _gel("queryText").innerHTML = txt;
    _gel("titleBox").style.display = 'block';
  } else {
    _gel("searchBox").style.display = 'block';
  }
}

function doEditSearch() {
  _gel("searchBox").style.display = 'block';
  _gel("titleBox").style.display = 'none';
}

// auto-adjust gadget height
gadgets.util.registerOnLoadHandler(function () {
  _gel("nxDocumentListData").innerHTML = '<p>' + prefs.getMsg('label.gadget.quicksearch.description') + '</p>';
  _gel("nxDocumentList").style.display = 'block';
  _gel('pageNavigationControls').style.display = 'none'

  loadSearch();
  gadgets.window.adjustHeight();
});

jQuery(document).ready(function () {
  jQuery('#searchPattern').keydown(function (event) {
    if (event.keyCode == '13') {
      doSearch();
    }
  });
});
