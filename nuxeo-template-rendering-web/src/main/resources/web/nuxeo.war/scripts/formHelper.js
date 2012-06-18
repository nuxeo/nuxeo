/**
 Retrieve Dom Elements for the matching formId/layoutId
**/
function getNuxeoFormFields(formId, layoutId) {
  var formElems = "input,select,textarea";
  if (formId) {
    var filter = "#" + formId;
    elems = jQuery(filter);
    if (layoutId) {
      rex = new RegExp(formId + ":nxl_" + layoutId + ":*");
      elems = elems.find(formElems).filter(function(){return this.id.match(rex);});
      return elems;
    } else {
      return elems.find(formElems);
    }
  } else {
     return jQuery("form").find(formElems);
  }
} 

function getFormInputValue(domInput) {
   if (domInput.tagName == "INPUT" && domInput.type == 'text' ) {
     return domInput.value;      
   }
   if (domInput.tagName == "INPUT" && domInput.type == 'hidden' ) {
     return domInput.value;      
   }
   if (domInput.tagName == "TEXTAREA" && domInput.type == 'text' ) {
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

function setFormInputValue(domInput, value) {
   if (domInput.tagName == "INPUT" && domInput.type == 'text' ) {
     domInput.value = value;      
   }
   else if (domInput.tagName == "INPUT" && domInput.type == 'hidden' ) {
     domInput.value = value;      
   }      
   else if (domInput.tagName == "TEXTAREA" && domInput.type == 'text' ) {
     domInput.value = value;      
   }
   else if (domInput.tagName == "INPUT" && domInput.type == 'radio' ) {      
     domInput.checked = value;
   }
   else if (domInput.tagName == "INPUT" && domInput.type == 'checkbox' ) {
     if (value==true) {
        domInput.checked = true;
     } else {
        jQuery(domInput).removeAttr("checked");
     }

   }
   else if (domInput.tagName == "SELECT" ) {
     jQuery(domInput).val(value);      
   }
 }

