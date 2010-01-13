function launchGadget(){
jQuery(document).ready(function(){
	jQuery('#show').click(function(){
		var callback = function(){
			jQuery('#show').hide();
			jQuery('#form').show();
			setWidthAndBindEvents();
			gadgets.window.adjustHeight();
		};
		gadgets.nuxeo.maximizeGadget("edit",callback);
	});
	
	
});
};


function launchEditGadget(){
jQuery(document).ready(function(){
	
	new nicEditor({iconsPath : '/nuxeo/site/gadgets/richtext/nicEditorIcons.gif', width : '500px'}).panelInstance('richtext');
	
	jQuery('#hide').click(function(){
		var callback = function(){
			jQuery('#form').hide();
			jQuery('#show').show();
			gadgets.window.adjustHeight();
		};
		gadgets.nuxeo.minimizeGadget(callback);
	});
	
});
};

var firstTime = true;
function setWidthAndBindEvents(){
	if(firstTime){
		var width = gadgets.window.getViewportDimensions().width;
		var area = jQuery('#richtext').prev();
		area.width(width);
		jQuery(area).prev().width(width);
		jQuery(area).keydown(function(){
		});
		firstTime = false;
	}
};