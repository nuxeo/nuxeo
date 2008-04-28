// helper functions
function navigationCB(url)
{
 url= getBaseURL() + url;
 //alert(url);
 window.location.href=url;
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

function doPast(docid)
{
 alert("past doc " + docid);
 refreshPage();
}

function doDelete(docid)
{
 alert("delete doc " + docid);
 refreshPage();
}

function doView(docid)
{
  navigateOnPopupDoc();
}

