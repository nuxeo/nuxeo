
tinyMCE.importPluginLanguagePack('nuxeoimageupload');

var TinyMCE_NuxeoImageUploadPlugin = {

  getControlHTML : function(cn) {
    switch (cn) {
      case "nuxeoimageupload":
        return tinyMCE.getButtonHTML(cn, 'lang_nuxeoimageupload_desc', '{$pluginurl}/images/icon.gif', 'mceNuxeoImageUpload');
    }

    return "";
  },

  /**
   * Executes the mceNuxeoImageUpload command.
   */
  execCommand : function(editor_id, element, command, user_interface, value) {
    // Handle commands
    switch (command) {
      case "mceNuxeoImageUpload":
        var template = new Array();

        var url = nxContextPath + '/editor_image_upload.faces' + '?conversationId=' + currentConversationId + '&conversationIsLongRunning=true';
        window.open(url, '_blank', 'toolbar=0, scrollbars=1, location=0, statusbar=0, menubar=0, resizable=1, dependent=1, width=350 , height=150');
        return true;
    }

    // Pass to next handler in chain
    return false;
  }
};

tinyMCE.addPlugin('nuxeoimageupload', TinyMCE_NuxeoImageUploadPlugin);
