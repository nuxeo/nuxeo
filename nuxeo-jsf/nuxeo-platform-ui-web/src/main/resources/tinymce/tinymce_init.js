var nbTinyMceEditor = 0;

// BBB
function initTinyMCE(width, height, eltId, plugins, lang, toolbar) {
  initTinyMCE(width, height, eltId, plugins, lang, toolbar, "{}");
}

// @since 8.1
function initTinyMCE(width, height, eltId, plugins, lang, toolbar, localConfString) {
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
        $parentForm.processRestore($parentForm.find("textarea.mceEditor,div.mce-edit-area>iframe"), data);
      });
    }
  }

  nbTinyMceEditor++;

  var defaultConf = {
    width : width,
    height : height,
    mode : "exact",
    theme : "modern",
    elements : eltId,
    plugins : [ "link image code searchreplace paste visualchars charmap table preview " + plugins ],
    language : lang,

    // Img insertion fixes
    relative_urls : false,
    remove_script_host : false,

    toolbar1 : "bold italic underline strikethrough | alignleft aligncenter alignright alignjustify | formatselect fontselect fontsizeselect",
    toolbar2 : "paste pastetext searchreplace | bullist numlist | outdent indent | undo redo | link unlink anchor image code",
    toolbar3 : "table | hr removeformat visualchars | subscript superscript | charmap preview | " + toolbar,
    menubar : false,

    setup : function(ed) {
      ed.on('init', function(ed) {
        loaded = true;
      });
    }
  };

  // merge default conf with conf passed via String property
  var localConf = JSON.parse(localConfString);
  var conf = jQuery.extend(true, {}, defaultConf, localConf);

  tinyMCE.init(conf);
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
  var escId = id.replace(/:/g, "\\:");
  jQuery('#' + escId).show();
}

function addTinyMCE(id) {
  tinyMCE.execCommand('mceAddEditor', false, id);
}

function resetTinyMCE(id) {
  var instance = tinyMCE.get(id);
  if (instance) {
    removeTinyMCE(id);
    // reinitialize TinyMCE with proper settings
    tinyMCE.init(instance.settings);
  }
  addTinyMCE(id);
}

function removeAllTinyMCEEditors() {
  for (var i = 0; i < tinyMCE.editors.length; i++) {
    try {
      tinyMCE.execCommand('mceRemoveEditor', false, tinyMCE.editors[i].id);
    } catch (e) {
      // ignore
    }
  }
  return true;
}
