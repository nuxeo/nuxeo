// import form popup
function showImportSetForm() {
  jQuery('<a href="#importSetFormPopup"></a>').fancybox({
    'autoScale'			: true,
    'transitionIn'		: 'none',
    'transitionOut'		: 'none',
    'enableEscapeButton': true,
    'centerOnScroll': true
  }).click();
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
  jQuery('<a href="#bulkEditPopup"></a>').fancybox({
    'autoScale'			: true,
    'transitionIn'		: 'none',
    'transitionOut'		: 'none',
    'enableEscapeButton': true,
    'centerOnScroll': true
  }).click();
}

// annotations
jQuery(document).ready(function() {
  jQuery("#showAnnotationsButton").fancybox({
    'width'				: '95%',
    'height'			: '95%',
    'autoScale'			: true,
    'transitionIn'		: 'none',
    'transitionOut'		: 'none',
    'type'				: 'iframe',
    'enableEscapeButton': true,
    'centerOnScroll': true
  });
});

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
