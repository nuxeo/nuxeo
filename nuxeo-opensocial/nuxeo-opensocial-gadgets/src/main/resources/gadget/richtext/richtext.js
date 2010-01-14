function launchGadget(){
jQuery(document).ready(function(){
	jQuery('#show').click(function(){
		jQuery('#show').hide();
		jQuery('#form').show();
		gadgets.window.adjustHeight();
	});
	
	jQuery('#hide').click(function(){
		jQuery('#form').hide();
		jQuery('#show').show();
		gadgets.window.adjustHeight();
	});
	
	new nicEditor({iconsPath : '/nuxeo/site/gadgets/richtext/nicEditorIcons.gif'}).panelInstance('richtext');
	
	setWidthAndBindEvents();
	jQuery('#loader').remove();
	gadgets.window.adjustHeight();
	
});
};


var firstTime = true;
function setWidthAndBindEvents(){
	if(firstTime){
		var width = gadgets.window.getViewportDimensions().width;
		var area = jQuery('#richtext').prev();
		area.css("background-color","white");
		area.width(width);
		jQuery(area).keydown(function(e){
			var keycode = (e.keyCode ? e.keyCode : (e.which ? e.which : e.charCode));
 			if (keycode == 13 || keycode == 8) 
				gadgets.window.adjustHeight();
      		return true;
		});
		jQuery('.nicEdit-main').width("100%");
		var prev = jQuery(area).prev();
		prev.width(width);
		firstTime = false;
	}
};