// import form popup
function showImportSetForm() {
  showFancyBox('#importSetFormPopup');
}

function processInProgress() {
  jQuery("#importSetFormPanelDiv").css("z-index", 10);
  jQuery("#importsetCreationWaiter").show();
}

function processFinished() {
  jQuery("#importSetFormPanelDiv").css("z-index", 1);
  jQuery("#importsetCreationWaiter").hide();
  jQuery.fancybox.close();
}

function fileUploadComplete() {
  jQuery("#importset_form\\:importSetFormOk").disabled = false;
  jQuery("#importset_form\\:importSetFormOk").removeAttr("disabled");
}

// bulk edit popup
function showBulkEditPopup() {
  showFancyBox('#bulkEditPopup');
}

// common
function togglePanel(button) {
  button = jQuery(button);
  var parent = button.parent();
  while (parent != null && !parent.hasClass('togglePanel')) {
    parent = parent.parent();
  }
  var ele = jQuery(parent.find('.togglePanelBody')[0]);
  ele.toggle();
  button.toggleClass('folded').toggleClass('unfolded');
  return false;
}
