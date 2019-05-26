(function($) {

function formSavedCallback() {
  jQuery.ambiance({
    title: nuxeo.safeEdit.feedbackMessage,
    className: "infoFeedback",
    timeout: 1.5
  });
}

$.fn.restoreDataCallbackPrompt = function(doLoadCB, key) {
  var confirm = jQuery('<a class="button smallButton" href="#">'
    + nuxeo.safeEdit.restorePrompt.confirmMessage + '</a>');
  confirm.click(function() {
    doLoadCB(true);
    jQuery("#confirmRestore").css({
      "display" : "none"
    });
    jQuery(".ambiance").remove();
    return false;
  }.bind(this));
  var discard = jQuery('<a href="#">' + nuxeo.safeEdit.restorePrompt.discardMessage + '</a>');
  discard.click(function() {
    doLoadCB(false);
    jQuery("#confirmRestore").css({
      "display" : "none"
    });
    jQuery(this).initSafeEditOnForm(key);
    jQuery(".ambiance").remove();
    return false;
  }.bind(this));

  jQuery.ambiance({
    title: nuxeo.safeEdit.restorePrompt.message,
    message: confirm.add(discard),
    className: "neutralFeedback",
    permanent: true
  });
}

$.fn.initSafeEditOnForm = function(key, message) {
  if (jQuery(this).size() > 0) {
    if (localStorage) {
      // leverage localStorage if available
      jQuery(this).initSafeEdit(key, 10 * 1000, formSavedCallback,
          function(doLoadCB) {
            return jQuery(this).restoreDataCallbackPrompt(doLoadCB, key);
          }.bind(this), message);
    } else {
      // limit to simple warn
      jQuery(this).detectDirtyPage(message);
    }
  }

}

$.fn.checkSafeEdit = function() {
  return jQuery(this).checkSafeEditOnForms(nuxeo.safeEdit.unsavedChangesMessage);
}

})(jQuery);
