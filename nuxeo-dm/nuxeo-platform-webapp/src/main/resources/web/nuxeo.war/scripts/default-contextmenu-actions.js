// helper functions
function navigationCB(url)
{
 if (url.indexOf("http")<0) {
   url= getBaseURL() + url;
 }
 window.location.href=url;
}

function trim (myString)
{
 return myString.replace(/^\s+/g,'').replace(/\s+$/g,'')
}

function navigateOnPopupDoc2(tabId, subTabId)
{
   Seam.Component.getInstance("popupHelper").getNavigationURLOnPopupdoc2(tabId,subTabId,navigationCB);
}

function navigateOnPopupDoc(tabId)
{
    Seam.Component.getInstance("popupHelper").getNavigationURLOnPopupdoc(tabId,navigationCB);
}

function navigateOnContainerDoc(tabId)
{
    Seam.Component.getInstance("popupHelper").getNavigationURLOnContainer(tabId,navigationCB);
}

function navigateOnDoc(docId, tabId)
{
    Seam.Component.getInstance("popupHelper").getNavigationURL(docId,tabId,navigationCB);
}

function refreshPage()
{
    Seam.Component.getInstance("popupHelper").getCurrentURL(navigationCB);
}

function reloadPage() {
  document.location.reload();
}

function refreshPageAfterDelete()
{
  Seam.Component.getInstance("popupHelper").getCurrentURLAfterDelete(navigationCB);
}

// menu actions callbacks
function doCopy(docid)
{
 Seam.Component.getInstance("clipboardActions").putInClipboard(docid,reloadPage);
}

function doPaste(docid)
{
    Seam.Component.getInstance("clipboardActions").pasteClipboardInside(docid,refreshPage);
}

function doMove(docid)
{
    Seam.Component.getInstance("clipboardActions").moveClipboardInside(docid,refreshPage);
}

function doDelete(docid)
{
  if (confirmDeleteDocuments()) {
    Seam.Component.getInstance("popupHelper").deleteDocument(docid,refreshPageAfterDelete);
    }
}

function doView(docid)
{
  navigateOnPopupDoc();
}

function doEdit(docid)
{
  navigateOnPopupDoc('TAB_EDIT');
}

function doRename(docid)
{
 span=document.getElementById('title_'+docid);
 input = document.createElement('input');
 if (span.id != '')
    input.id = span.id;
 input.className = span.className;
 span.parentNode.replaceChild(input,span);
 input.value = trim(span.innerHTML);
 input.onkeydown = function (event) {
        onEditKeyPress(event);
 };
 input.focus();
}

function doAccessRights(docid) {
  navigateOnPopupDoc('TAB_PERMISSIONS');
}

function doPreview(docid) {
  navigateOnPopupDoc('TAB_PREVIEW');
}

function doPreviewPopup(docid) {
  Seam.Component.getInstance("previewActions").getPreviewPopupURL(docid, function(result) {
    openFancyBox(result);
  });
}

function doDownload(docid) {
  Seam.Component.getInstance("popupHelper").downloadDocument(docid, 'file:content', 'file:filename', navigationCB);
}

function doWorkflow(docid) {
  navigateOnPopupDoc('TAB_CONTENT_JBPM');
}

function doLock(docid){
  Seam.Component.getInstance("popupHelper").lockDocument(docid, refreshPage);
}

function doUnlock(docid){
  Seam.Component.getInstance("popupHelper").unlockDocument(docid, refreshPage);
}

function doSendEmail(docid){
  Seam.Component.getInstance("popupHelper").sendEmail(docid, navigationCB);
}


function onEditKeyPress(event)
{
if(!event) event = window.event;
var keyCode = (event.which) ? event.which: event.keyCode;
var target = (event.target) ? event.target: event.srcElement;
if (keyCode == 13)
 saveRename(target);
}

function saveRenameCallback(result)
{
 if (result!="OK")
   refreshPage()
}

function saveRename(target)
{
 input = target;
 span = document.createElement('span');
 input.parentNode.replaceChild(span,input);
 span.innerHTML = input.value;
 span.className = input.className;
 if (input.id != '')
    span.id = input.id;

 docid = span.id.replace('title_','');
 Seam.Component.getInstance("popupHelper").editTitle(docid,input.value,saveRenameCallback);
}

