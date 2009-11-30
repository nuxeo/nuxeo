var ctrlClickSelectedClassName = "selectedCtrlClick";

function toggleCtrlClickSelection(e) {
  if(jQuery(e).hasClass(ctrlClickSelectedClassName)) {
    jQuery(e).removeClass(ctrlClickSelectedClassName);
  } else {
    jQuery(e).addClass(ctrlClickSelectedClassName);
  }
}

function isCtrlClickSelected(e) {
  return jQuery(e).hasClass(ctrlClickSelectedClassName);
}


function ctrlClickSelectionCB(element) {
  return function(result) {
    if(typeof(result) != "undefined") {
      if (result.indexOf("ERROR") == 0) {
        // should never occur if the application if providers are configured
        // properly
        alert(result);
      } else {
        toggleCtrlClickSelection(element);
      }
    }
  };
}

function installCtrlClickSelectEventListeners() {
  //console.log("installing installCtrlClickSelectEventListeners");
  // install selection event handlers for the  ctrl click event
  jQuery("div.thumbViewElement").click(function(e) {
    if (e.metaKey || e.ctrlKey) {
    var documentActions = Seam.Component.getInstance("documentActions");
    var tagToToggle = jQuery(this);
    documentActions.processSelectRow(tagToToggle.attr('docref'),
      tagToToggle.attr('providername'), 'CURRENT_SELECTION', !isCtrlClickSelected(tagToToggle),
      ctrlClickSelectionCB(tagToToggle));

    // prevent further propagation to avoid global unselect
    e.stopPropagation();
    }
  });
}

function installCtrlClickUnSelectEventListeners() {
  //console.log("installing installCtrlClickUnSelectEventListeners");
  // install selection event handlers for the  ctrl click event
  jQuery("#dataTableContainer").click(function(e) {
    if (!e.metaKey && !e.ctrlKey) {
    // clear  the working list on the server side so that when paging
    // nothing is selected
    Seam.Component.getInstance("bulkSelectActions").clearWorkingList('CURRENT_SELECTION');
    jQuery(".thumbViewElement").each(function (i) {
      // remove the selectedCtrlClick class from thumbnails on the current page
      if(jQuery(this).hasClass("selectedCtrlClick")) {
        jQuery(this).removeClass("selectedCtrlClick");
      }
    });
    };
  });
}

// init is down inside the page
//jQuery(document).ready(function(){
//  installCtrlClickSelectEventListeners();
//  installCtrlClickUnSelectEventListeners();
//});

