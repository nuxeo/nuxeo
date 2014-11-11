function formSavedCallback() {
  jQuery.ambiance({
    title: nuxeo.safeEdit.feedbackMessage,
    className: "infoFeedback",
    timeout: 1.5
  });
}

function restoreDataCallbackPrompt(doLoadCB, formId, key) {
  var confirm = jQuery('<a class="button smallButton" href="#">'
    + nuxeo.safeEdit.restorePrompt.confirmMessage + '</a>');
  confirm.click(function() {
    doLoadCB(true), jQuery("#confirmRestore").css({
      "display" : "none"
    });
    jQuery(this).parent(".ambiance").remove();
    return false;
  });
  var discard = jQuery('<a href="#">' + nuxeo.safeEdit.restorePrompt.discardMessage + '</a>');
  discard.click(function() {
    doLoadCB(false), jQuery("#confirmRestore").css({
      "display" : "none"
    });
    initSafeEditOnForm(formId, key);
    jQuery(this).parent(".ambiance").remove();
    return false;
  });

  jQuery.ambiance({
    title: nuxeo.safeEdit.restorePrompt.message,
    message: confirm.add(discard),
    className: "neutralFeedback",
    permanent: true
  });
}

function initSafeEditOnForm(formId, key, message) {
  if (!formId.startsWith('#')) {
    formId = "#" + formId;
  }

  if (jQuery(formId).size() > 0) {
    if (localStorage) {
      // leverage localStorage if available
      initSafeEdit(key, formId, 10 * 1000, formSavedCallback,
          function(doLoadCB) {
            return restoreDataCallbackPrompt(doLoadCB, formId, key);
          }, message);
    } else {
      // limit to simple warn
      detectDirtyPage(formId, message);
    }
  }

}
