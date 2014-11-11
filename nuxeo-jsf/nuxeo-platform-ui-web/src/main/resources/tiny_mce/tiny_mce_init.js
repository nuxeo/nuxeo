    parameters = {width : width, 
                  height : height, 
                  editor_selector : editorSelector, 
                  mode : "textareas", 
                  plugins : plugins,
                  language : lang,
                 }
	
    for (key in toolbarOptions) {
        parameters[key] = toolbarOptions[key];
    }
    tinyMCE.init(parameters);