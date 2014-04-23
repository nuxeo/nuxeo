var nbTinyMceEditor = 0;

function initTinyMCE(width, height, eltId, plugins, lang, toolbar) {
  var loaded = false;
  var $el = jQuery(document.getElementById(eltId));
  if ($el.hasClass("disableMCEInit")) {
    return;
  }
  // force English by default since there are no translations for other
  // languages than en and fr
  if (lang != 'en' && lang != 'fr') {
    lang = 'en';
  }

  // if SafeEdit present
  var $parentForm = jQuery($el).closest("form");
  if ($parentForm.hasClass("safeEditEnabled")) {
    // register wait for this editor
    var $parentForm = $el.closest('form');
    $parentForm.registerSafeEditWait(function() {
      return loaded;
    });

    if (nbTinyMceEditor == 0) {
      // For safeEdit: register post restore
      // The post restore is common for all editors of the page, so just
      // register once
      $parentForm.registerPostRestoreCallBacks(function(data) {
        $parentForm.processRestore($parentForm
            .find("textarea.mceEditor,div.mce-edit-area>iframe"), data);
      });
    }
  }

  nbTinyMceEditor++;

  tinyMCE
      .init({
        width : width,
        height : height,
        mode : "exact",
        theme : "modern",
        elements : eltId,
        plugins : ["link image code searchreplace paste visualchars charmap table fullscreen preview nuxeoimageupload nuxeolink"],
        language : lang,

        // Img insertion fixes
        relative_urls : false,
        remove_script_host : false,

        toolbar1 : "bold italic underline strikethrough | alignleft aligncenter alignright alignjustify | formatselect fontselect fontsizeselect",
        toolbar2 : "paste pastetext searchreplace | bullist numlist | outdent indent | undo redo | link unlink anchor image code",
        toolbar3 : "table | hr removeformat visualchars | subscript superscript | charmap preview | fullscreen nuxeoimageupload nuxeolink",
        menubar: false,
        
        // TODO : upgrade the skin "o2k7" with the new version
        /*skin : "o2k7",
        skin_variant : "silver",
        theme_advanced_disable : "styleselect",
        theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap,|",
        theme_advanced_buttons3_add : toolbar, */
        
        setup : function(ed) {
          ed.on('init', function(ed) {
            loaded = true;
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
  tinyMCE.execCommand('mceRemoveEditor', false, id);
}

function addTinyMCE(id) {
  tinyMCE.execCommand('mceAddEditor', false, id);
}

function removeAllTinyMCEEditors() {
  for ( var i = 0; i < tinyMCE.editors.length; i++) {
    try {
      tinyMCE.execCommand('mceRemoveEditor', false, tinymce.editors[i].id);
    } finally {
    }
  }
  ;
  return true;
}
