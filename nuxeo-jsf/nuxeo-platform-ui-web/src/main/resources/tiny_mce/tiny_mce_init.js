//Language configuration
if(lang!= 'en' && lang !='fr'){
	lang = 'en';
}
tinyMCE.init({
		width : width,
        height : height,
        mode : "specific_textareas",
        theme : "advanced",
		editor_selector : editorSelector,
		editor_deselector : "disableMCEInit",
        plugins : plugins,
		language : lang,
		theme_advanced_resizing : true,
		
		//Img insertion fixes
		relative_urls : false,
		remove_script_host : false,
        skin : "o2k7",
        skin_variant : "silver",
    theme_advanced_disable : "styleselect",
		theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap,|",
		theme_advanced_buttons3_add : toolbar
});

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