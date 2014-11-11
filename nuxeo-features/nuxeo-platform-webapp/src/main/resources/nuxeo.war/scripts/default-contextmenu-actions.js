// helper functions
function navigationCB(url)
{
 url= getBaseURL() + url;
 window.location.href=url;
}

function trim (myString)
{
 return myString.replace(/^\s+/g,'').replace(/\s+$/g,'')
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

// menu actions callbacks
function doCopy(docid)
{
 Seam.Component.getInstance("clipboardActions").putInClipboard(docid,refreshPage);
}

function doPaste(docid)
{
	Seam.Component.getInstance("clipboardActions").pasteClipboardInside(docid,refreshPage);
}

function doDelete(docid)
{
	if (confirmDeleteDocuments()) {
		Seam.Component.getInstance("popupHelper").deleteDocument(docid,refreshPage);
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

function onEditKeyPress(event)
{
if (event.keyCode == 13)
 saveRename(event);
}

function saveRenameCallback(result)
{
 if (result!="OK")
   refreshPage()
}

function saveRename(event)
{
 input = event['target'];
 span = document.createElement('span');
 input.parentNode.replaceChild(span,input);
 span.innerHTML = input.value;
 span.className = input.className;
 if (input.id != '')
	span.id = input.id;

 docid = span.id.replace('title_','');
 Seam.Component.getInstance("popupHelper").editTitle(docid,input.value,saveRenameCallback);
}

