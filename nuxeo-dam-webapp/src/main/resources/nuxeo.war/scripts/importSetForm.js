function processInProgress() {
  //alert('Process In progress');
  document.getElementById('importSetFormPanelDiv').style.zIndex=10;
  document.getElementById('importsetCreationWaiter').style.display="inline";
}

function processFinished() {
  //alert('Process finished');
  document.getElementById('importSetFormPanelDiv').style.zIndex=1;
  Richfaces.hideModalPanel('importSetFormPanel');
  document.getElementById('importsetCreationWaiter').style.display="none";
}

function fileUploadComplete() {
  //alert('File Upload complete');
  document.getElementById("importset_form:importSetFormOk").disabled = false;
}

function initImportSetForm() {
  //alert('Initialize ImportSet Form');
  document.getElementById('importset_form:importSetFormOk').disabled = true;
}

function hideWaiter() {
  document.getElementById('importSetFormPanelDiv').style.zIndex=1; 
  document.getElementById('importsetCreationWaiter').style.display='none';
}