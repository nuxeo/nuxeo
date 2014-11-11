
tinyMCE.importPluginLanguagePack('nuxeolink');

var TinyMCE_NuxeoLinkPlugin = {

  getControlHTML : function(cn) {
    switch (cn) {
      case "nuxeolink":
        return tinyMCE.getButtonHTML(cn, 'lang_nuxeolink_desc', '{$pluginurl}/images/icon.gif', 'mceNuxeoLink');
    }

    return "";
  },

  /**
   * Executes the mceEmotion command.
   */
  execCommand : function(editor_id, element, command, user_interface, value) {
    // Handle commands
    switch (command) {
      case "mceNuxeoLink":
        var template = new Array();

        window.open(nxContextPath + '/editor_link_search_document.faces', '_blank', 'toolbar=0, scrollbars=1, location=0, statusbar=0, menubar=0, resizable=1, dependent=1, width=800, height=480');
        return true;
    }

    // Pass to next handler in chain
    return false;
  }
};

tinyMCE.addPlugin('nuxeolink', TinyMCE_NuxeoLinkPlugin);