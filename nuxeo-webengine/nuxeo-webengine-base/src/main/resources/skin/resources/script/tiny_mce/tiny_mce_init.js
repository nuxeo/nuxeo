tinyMCE.init({
        // General options
        mode : "specific_textareas",
        theme : "advanced",
		editor_selector : "mceEditor",
		editor_deselector : "disableMCEInit",
		language: 'en',
		theme_advanced_resizing : true,

		relative_urls : true,

        // Skin options
        skin : "o2k7",
        skin_variant : "silver",

    theme_advanced_disable : "styleselect",
		theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap"

});