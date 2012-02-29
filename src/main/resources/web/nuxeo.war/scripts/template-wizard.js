
 function getHiddenFormFields() {
  var filter = "#nxl_" + wizard_params.advancedLayout;
  return jQuery(filter).find("input,select");
 } 

 function getInputValue(domInput) {
   if (domInput.tagName == "INPUT" && domInput.type == 'text' ) {
     return domInput.value;      
   }
   if (domInput.tagName == "INPUT" && domInput.type == 'radio' ) {
     return domInput.checked;      
   }
   if (domInput.tagName == "INPUT" && domInput.type == 'checkbox' ) {
     return domInput.checked;      
   }
   if (domInput.tagName == "SELECT" ) {
     return jQuery(domInput).val();
   }
 }

 function setInputValue(domInput, value) {
   if (domInput.tagName == "INPUT" && domInput.type == 'text' ) {
     domInput.value = value;      
   }
   if (domInput.tagName == "INPUT" && domInput.type == 'radio' ) {      
     domInput.checked = value;
   }
   if (domInput.tagName == "INPUT" && domInput.type == 'checkbox' ) {
     if (value==true) {
        domInput.checked = true;
     } else {
        jQuery(domInput).removeAttr("checked");
     }

   }
   if (domInput.tagName == "SELECT" ) {
     jQuery(domInput).val(value);      
   }
 }
 function saveForm() {
   jQuery(getHiddenFormFields()).each( function() {
         jQuery(this).attr("nxw_data",getInputValue(this));
   });
 }

 function restoreFormState() {
   jQuery(getHiddenFormFields()).each( function() {
         setInputValue(this, jQuery(this).attr("nxw_data"));
   });
 }
 
 function selectPreset(name) {
    //resetForm();    
    restoreFormState();
    var wParam = wizard_params.mapping[name];
    for (var wIdx = 0; wIdx < wParam.length; wIdx++) {
      var widget = wParam[wIdx]; 
      var item = jQuery("#" + wizard_params.form + "\\:nxl_" + wizard_params.advancedLayout + "\\:nxw_" + widget.id);
      if (widget.val) {
        item.val(widget.val);
      }  else {
        item.attr("checked", widget.checked);
      }
    }      
 }
  
jQuery(document).ready(function(){

  saveForm();

  // bind mode selector container
  ;
  jQuery(jQuery('.modeSelectorContainer')[0]).css("backgroundColor", "#DDDDDD");
  jQuery('.modeSelectorContainer').css("cursor","pointer").click( function() {
    if (this.tagName!="INPUT") {
       var targetInput = jQuery(this).children("input[@type=radio]")[0];
       jQuery(targetInput).click();
       targetInput.checked=true;
    }
    });
  
  // bind mode selector
  jQuery('.modeSelector').click( function(){
     var mode = jQuery(this).val();
     jQuery(".modeSelectorContainer").css("backgroundColor", "white");
     jQuery(this).parent(".modeSelectorContainer").css("backgroundColor", "#DDDDDD");
     var wizardPanel = jQuery("#" + wizard_params.presetLayout);
     var advancedPanel = jQuery("#nxl_" + wizard_params.advancedLayout);
     if (mode=='preset') {
         advancedPanel.fadeOut( 300, function() {
            advancedPanel.css("display","none");
            wizardPanel.fadeIn('fast');
          });         
     } else {
         wizardPanel.fadeOut( 300, function() {
            wizardPanel.css("display","none");
            advancedPanel.fadeIn('fast');
          });
     }
     return false;
  });

    // change CSS on presetSelector
    jQuery('.presetSelector').css("cursor","pointer");

    // bind presetSelector
    jQuery('.presetSelector').click( function(){
      jQuery('li.presetSelector').css("backgroundColor", "white");
      jQuery(this).css("backgroundColor", "#DDDDDD");
      var inputOption = jQuery(jQuery(this).children("input[@type=radio]")[0]); 
      var presetName =inputOption.val();     
      selectPreset(presetName);
      inputOption.attr("checked","checked");
    });
});
