
 function wizard_getHiddenFormFields() {
  var filter = "#nxl_" + wizard_params.advancedLayout;
  return jQuery(filter).find("input,select");
 } 

 function wizard_saveForm() {
   jQuery(wizard_getHiddenFormFields()).each( function() {
         jQuery(this).attr("nxw_data",getFormInputValue(this));
   });
 }

 function wizard_restoreFormState() {
   jQuery(wizard_getHiddenFormFields()).each( function() {
         setFormInputValue(this, jQuery(this).attr("nxw_data"));
   });
 }
 
 function wizard_selectPreset(name) {
    wizard_restoreFormState();
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

function wizard_selectMode(radio) {
     var mode = jQuery(radio).val();
     jQuery(".modeSelectorContainer").css("backgroundColor", "white");
     jQuery(radio).parent(".modeSelectorContainer").css("backgroundColor", "#DDDDDD");
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
     jQuery(radio).attr('checked',true);
  }
  
jQuery(document).ready(function(){

  wizard_saveForm();

  // bind mode selector container  
  jQuery(jQuery('.modeSelectorContainer')[0]).css("backgroundColor", "#DDDDDD");
  jQuery('.modeSelectorContainer').css("cursor","pointer").click( function() {
    if (this.tagName!="INPUT") {
       var targetInput = jQuery(this).children("input[@type=radio]")[0];
       wizard_selectMode(targetInput);
    }
    return true});
  

  // bind mode selector
  jQuery('.modeSelector').click( function(){    
     wizard_selectMode(this)     
     return true;
  });

  // change CSS on presetSelector
  jQuery('.presetSelector').css("cursor","pointer");

  // bind presetSelector
  jQuery('.presetSelector').click( function(){
      jQuery('li.presetSelector').css("backgroundColor", "white");
      jQuery(this).css("backgroundColor", "#DDDDDD");
      var inputOption = jQuery(jQuery(this).children("input[@type=radio]")[0]); 
      var presetName =inputOption.val();     
      wizard_selectPreset(presetName);
      inputOption.attr("checked","checked");
    });
});



