var prefs = new gadgets.Prefs();

// configure Automation REST call
var NXRequestParams = {
  operationId: 'Collection.GetElementsInFavorite',
  operationParams: {},
  operationContext: {},
  operationInput: getTargetContextPath(),
  operationDocumentProperties: "common,dublincore",
  entityType: 'documents',
  usePagination: true,
  displayMethod: displayDocumentList,
  displayColumns: [
    {type: 'builtin', field: 'icon'},
    {type: 'builtin', field: 'titleWithLink', label: prefs.getMsg('label.dublincore.title')},
    {type: 'date', field: 'dc:modified', label: prefs.getMsg('label.dublincore.modified')},
    {type: 'text', field: 'dc:creator', label: prefs.getMsg('label.dublincore.creator')}
  ],
  noEntryLabel: prefs.getMsg('label.gadget.no.document')
};

// execute automation request onload
gadgets.util.registerOnLoadHandler(function() {
  initContextPathSettingsButton();
  doAutomationRequest(NXRequestParams);
});