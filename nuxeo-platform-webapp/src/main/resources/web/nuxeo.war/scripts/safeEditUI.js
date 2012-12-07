    function formSavedCallback(data) {
      jQuery("#savedFeedback").css({"display" : "inline"});
      window.setTimeout(function(){ jQuery("#savedFeedback").css({"display" : "none"}); }, 1500);
    }

    function restoreDataCallbackPrompt(doLoadCB, formId, key) {
      jQuery("#confirmRestoreYes").click(function() {
         doLoadCB(true),jQuery("#confirmRestore").css({"display" : "none"});return false;
       });
      jQuery("#confirmRestoreNo").click(function() {
         doLoadCB(false),jQuery("#confirmRestore").css({"display" : "none"});
         initSafeEditOnForm(formId, key);
         return false;
       });
      jQuery("#confirmRestore").css({"display" : "inline"});
    }

    function initSafeEditOnForm(formId, key) {

       if (!formId.startsWith('#')) {
         formId= "#" + formId;
       }
       if (jQuery(formId).size()>0) {
         initSafeEdit(key,formId,10*1000, formSavedCallback,
            function(doLoadCB){return restoreDataCallbackPrompt(doLoadCB, formId, key);});
       }
       else {
         //console.log("No target form was found for id " + formId);
       }
    }

