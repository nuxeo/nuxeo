var nbTinyMceEditor = 0;

function initTinyMCE(width, height, eltId, plugins, lang, toolbar) {
  var escId = eltId.replace(/:/g, "\\:");
  if (jQuery("#" + escId).hasClass("disableMCEInit")) {
    return;
  }
  // force English by default since there are no translations for other
  // languages than en and fr
  if (lang != 'en' && lang != 'fr'){
    lang = 'en';
  }

  // if SafeEdit present
  if (typeof registerSafeEditWait == 'function') {
    // tell him to wait until all editors are loaded
    registerSafeEditWait(function() {
      return nbTinyMceEditor <= 0;
    });
  }

  // if SafeEdit present
  if (typeof registerPostRestoreCallBacks == 'function') {
    // tell him to run a restore again on the following elts
    if (nbTinyMceEditor == 0) {
      // The post restore is common for all editors of the page, so just
      // register once
      registerPostRestoreCallBacks(function(formSelector, data) {
        processRestore(jQuery(formSelector).find(
            "textarea.mceEditor,td.mceIframeContainer>iframe"), data);
      });
    }
  }

  nbTinyMceEditor++;


  tinyMCE.init({
    width : width,
    height : height,
    mode : "exact",
    theme : "advanced",
    elements : eltId,
    plugins : plugins,
    language : lang,
    theme_advanced_resizing : true,

    // Img insertion fixes
    relative_urls : false,
    remove_script_host : false,
    skin : "o2k7",
    skin_variant : "silver",
    theme_advanced_disable : "styleselect",
    theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap,|",
    theme_advanced_buttons3_add : toolbar,
    setup : function(ed) {
      ed.onInit.add(function(ed) {
          nbTinyMceEditor--;
          // XXX Make sure this work when we have several editors
      });
   }
  });
}

function toggleTinyMCE(id) {
  if (!tinyMCE.getInstanceById(id)) {
    addTinyMCE(id);
  } else {
    removeTinyMCE(id);
  }
}

function removeTinyMCE(id) {
 tinyMCE.execCommand('mceRemoveControl', false, id);
}

function addTinyMCE(id) {
 tinyMCE.execCommand('mceAddControl', false, id);
}

function removeAllTinyMCEEditors() {
  for (var i=0; i < tinyMCE.editors.length; i++) {
     try {
       tinyMCE.execCommand('mceRemoveControl',false, tinymce.editors[i].id);
     } finally {
     }
  };
  return true;
}