function processInProgress() {
  jQuery("#importSetFormPanelDiv").css("z-index", 10);
  jQuery("#importsetCreationWaiter").show();
}

function processFinished() {
  jQuery("#importSetFormPanelDiv").css("z-index", 1);
  Richfaces.hideModalPanel('importSetFormPanel');
  jQuery("#importsetCreationWaiter").hide();
}

function fileUploadComplete() {
  jQuery("#importset_form\\:importSetFormOk").disabled = false;
  jQuery("#importset_form\\:importSetFormOk").removeAttr("disabled");
}

function hideWaiter() {
  jQuery("#importSetFormPanelDiv").css("z-index", 1);
  jQuery("#importsetCreationWaiter").hide();
}
