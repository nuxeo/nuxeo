
    parameters = {width : width,
                  height : height,
                  editor_selector : editorSelector,
                  editor_deselector : "disableMCEInit",
                  mode : "textareas",
                  plugins : plugins,
                  language : lang};

    for (key in toolbarOptions) {
        parameters[key] = toolbarOptions[key];
    }
    tinyMCE.init(parameters);

    function toggleTinyMCE(id) {
      if (!tinyMCE.getInstanceById(id))
        addTinyMCE(id);
       else
        removeTinyMCE(id);
      }

    function removeTinyMCE(id) {
     tinyMCE.execCommand('mceRemoveControl', false, id);
    }

    function addTinyMCE(id) {
     tinyMCE.execCommand('mceAddControl', false, id);
    }
