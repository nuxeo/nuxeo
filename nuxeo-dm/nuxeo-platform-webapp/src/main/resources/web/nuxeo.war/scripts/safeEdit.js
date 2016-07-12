(function($) {

  var waitPeriod = 100;
  // How long are we gonna wait for widgets to load
  // Here is 40 x 100ms = 4sec
  var maxWaitingIteration = 30;

  var saveFormFunc;

  function getInputValue(domInput) {
    if (domInput.tagName == "INPUT") {
      if (domInput.type == 'text' || domInput.type == 'hidden') {
        return domInput.value;
      } else if (domInput.type == 'radio' || domInput.type == 'checkbox') {
        return domInput.checked;
      }
    } else if (domInput.tagName == "SELECT") {
      return jQuery(domInput).val();
    } else if (domInput.tagName == "TEXTAREA") {
      return jQuery(domInput).val();
    } else if (domInput.tagName == "IFRAME") {
      return jQuery(domInput).contents().find("body").html();
    }
  }

  function setInputValue(domInput, value) {

    if (domInput.tagName == "INPUT") {
      if (domInput.type == 'text' || domInput.type == 'hidden') {
        domInput.value = value;
      } else if (domInput.type == 'radio') {
        if (value == true || value == "true") {
          domInput.checked = true;
        } else {
          jQuery(domInput).removeAttr("checked");
        }
      } else if (domInput.type == 'checkbox') {
        if (value == true) {
          domInput.checked = true;
        } else {
          jQuery(domInput).removeAttr("checked");
        }
      }
    } else if (domInput.tagName == "SELECT" || domInput.tagName == "TEXTAREA") {
      jQuery(domInput).val(value).change();
    } else if (domInput.tagName == "IFRAME") {
      return jQuery(domInput).contents().find("body").html(value);
    }
  }

  function mustSkipField(field) {
    if (field.type == 'button' || field.type == 'submit') {
      return true;
    }
    if (field.name == 'javax.faces.ViewState') {
      return true;
    }
    return false;
  }

  function registerSafeEditForm(formId) {
    var $root = jQuery(":root");
    if ($root.data('safeEditForms') === undefined) {
      $root.data('safeEditForms', []);
    }
    if (jQuery.inArray(formId, $root.data('safeEditForms')) == -1) {
      $root.data('safeEditForms').push(formId);
    }
  }

  $.fn.checkSafeEditOnForms = function(message) {
    var $root = jQuery(":root");
    var formIds = $root.data('safeEditForms');
    if (formIds != undefined) {
      for ( var i = 0, len = formIds.length; i < len; i++) {
        if (formIds[i] == undefined) {
          continue;
        }
        var formId = formIds[i].split(":").join("\\:")
        if (jQuery("#" + formId).data("dirtyPage")) {
          var r = confirm(message);
          if (r == true) {
            jQuery("#" + formId).data('dirtyPage', false);
            jQuery(window).unbind('beforeunload');
            jQuery("#" + formId).cleanupSavedData();
          }
          return r;
        }
      }
    }
    return true;
  }

  $.fn.saveForm = function() {
    jQuery(this).saveForm(0, null);
  }

  $.fn.getFormItems = function() {
    return jQuery(this)
        .find(
            "input:not(.select2-input),select,textarea,div.mce-edit-area>iframe");
  }

  $.fn.collectFormData = function() {
    var data = {};
    jQuery(this).getFormItems().each(function() {
      if (!mustSkipField(this)) {
        var key = this.id;
        if (!key) {
          key = this.name;
        }
        data[key] = getInputValue(this);
      }
    });
    return data;
  }

  $.fn.saveForm = function(savePeriod, saveCB) {
    var data = jQuery(this).collectFormData();
    var dataToStore = JSON.stringify(data);
    var $form = jQuery(this);
    if (dataToStore == $form.data('lastSavedJSONData')) {
      //console.log("skip save ... no change");
    } else {
      localStorage.setItem(jQuery(this).data('key'), dataToStore);
      $form.data('lastSavedJSONData', dataToStore);
      if (saveCB != null) {
        saveCB(data);
      }
    }
    if (savePeriod > 0 && !$form.data('blockAutoSave')) {
      saveFormFunc = window.setTimeout(function() {
        $form.saveForm(savePeriod, saveCB)
      }, savePeriod);
    }
    return data;
  }

  $.fn.cleanupSavedData = function() {
    //console.log("Cleanup custom storage");
    localStorage.removeItem(jQuery(this).data('key'));
    window.clearTimeout(saveFormFunc);
  }

  $.fn.processRestore = function(elts, data) {
    elts.each(function() {
      if (!mustSkipField(this)) {
        var k = this.id;
        if (!k) {
          k = this.name;
        }
        setInputValue(this, data[k]);
      }
    });
  }

  $.fn.restoreDraftFormData = function(loadCB, savePeriod, saveCB) {
    var dataStr = localStorage.getItem(jQuery(this).data('key'));
    if (dataStr) { // there is some saved data

      var currentData = JSON.stringify(jQuery(this).collectFormData());
      // console.log("restore check", currentData, dataStr);
      if (currentData == dataStr) {
        // don't propose to restore if there is nothing new !
        return false;
      }

      var $form = jQuery(this);
      // block auto save until use choose to restore or not
      $form.data('blockAutoSave', true);
      // build load callback that UI will call if user wants to restore
      var doLoad = function(confirmLoad) {
        if (confirmLoad) {
          // restore !
          var data = JSON.parse(dataStr);
          $form.processRestore($form.getFormItems(), data);

          // Any post restore actions?
          $form.processPostRestore(data);
        } else {
          // drop saved data !
          $form.cleanupSavedData();
        }
        $form.data('blockAutoSave', false);
        if (savePeriod > 0) {
          saveFormFunc = window.setTimeout(function() {
            $form.saveForm(savePeriod, saveCB)
          }, savePeriod);
        }
      };
      if (loadCB != null) {
        if (!loadCB(doLoad)) {
          return true;
        }
      } else {
        doLoad();
      }
      return true;
    }
    return false;
  }

  $.fn.bindOnChange = function(cb) {

    jQuery(this).getFormItems().each(function() {
      var targetDomItem = jQuery(this);
      if (this.tagName == "IFRAME") {
        targetDomItem = jQuery(this).contents().find("body");
        targetDomItem.bind("DOMSubtreeModified", cb);
      } else {
        targetDomItem.change(cb);
      }
    });
  }

  $.fn.detectDirtyPage = function(message) {
    jQuery(this).bindOnChange(function(event) {
      if (!jQuery(this).data('dirtyPage')) {
        jQuery(window).bind('beforeunload', function() {
          return message;
        });
      }
      jQuery(this).data('dirtyPage', true);
    });
    jQuery(this).submit(function() {
      jQuery(this).data('dirtyPage', false);
      jQuery(window).unbind('beforeunload');
      return true;
    })
  }

  $.fn.processPostRestore = function(data) {
    var postRestoreFunctions = jQuery(this).data('postRestoreFunctions');
    if (postRestoreFunctions !== undefined) {
      for ( var i = 0, len = postRestoreFunctions.length; i < len; i++) {
        postRestoreFunctions[i](data);
      }
    }
  }

  $.fn.registerSafeEditWait = function(waitFct) {
    var $form = jQuery(this);
    if ($form.data('waitFunctions') === undefined) {
      $form.data('waitFunctions', []);
    }
    $form.data('waitFunctions').push(waitFct);
  }

  $.fn.registerPostRestoreCallBacks = function(postRestoreFct) {
    var $form = jQuery(this);
    if ($form.data('postRestoreFunctions') === undefined) {
      $form.data('postRestoreFunctions', []);
    }
    $form.data('postRestoreFunctions').push(postRestoreFct);
  }

  $.fn.initSafeEdit = function(key, savePeriod, saveCB, loadCB, message) {

    var $form = jQuery(this);

    doInitSafeEdit = function(savePeriod, saveCB, loadCB, message) {
      var loaded = $form.restoreDraftFormData(loadCB, savePeriod, saveCB);
      $form.bindOnChange(function(event) {
        if (!$form.data('dirtyPage')) {
          // first time we detect a dirty page, we start force save
          $form.saveForm(savePeriod, saveCB);
          jQuery(window).bind('beforeunload', function() {
            // return message
            return message;
          });
          // if the user really wanna leave the page, then we clear the
          // localstorage
          jQuery(window).bind('unload', function() {
            $form.cleanupSavedData();
          });
        }
        $form.data('dirtyPage', true);
      });
      $form.submit(function() {
        $form.data('dirtyPage', false);
        jQuery(window).unbind('beforeunload');
        $form.cleanupSavedData();
        return true;
      });
      // register safe edit form to the root
      registerSafeEditForm($form.attr("id"));
    }

    initWhenPageReady = function(savePeriod, saveCB, loadCB, message,
        waitFunctionIndex) {
      var waitFunctions = $form.data('waitFunctions');
      var currentWaitingIteration = $form.data('currentWaitingIteration');
      if (waitFunctions === undefined
          || waitFunctionIndex > waitFunctions.length - 1
          || currentWaitingIteration > maxWaitingIteration) {
        // Nothing to wait, lets' go!
        doInitSafeEdit(savePeriod, saveCB, loadCB, message);
      } else {
        var stillWaiting = !(waitFunctions[waitFunctionIndex]());
        if (stillWaiting) {
          // Something is still loading, let's give it more time (i.e.
          // waitPeriod)
          // console.debug('waiting ... ');
          currentWaitingIteration++;
          $form.data('currentWaitingIteration', currentWaitingIteration);
          window.setTimeout(function() {
            initWhenPageReady(savePeriod, saveCB, loadCB, message,
                waitFunctionIndex);
          }, waitPeriod);
        } else {
          // The thing we were waiting for has finished to load, let's wait for
          // the next one
          initWhenPageReady(savePeriod, saveCB, loadCB, message,
              waitFunctionIndex + 1);
        }
      }
    }

    if ($form.data('safeEditInitDone') !== true) {

      $form.data('safeEditInitDone', true);

      // Set control data in the form
      $form.data('blockAutoSave', false);
      $form.data('dirtyPage', false);
      $form.data('lastSavedJSONData', null);
      $form.data('currentWaitingIteration', 0);
      $form.data('key', key);

      // Perform init
      initWhenPageReady(savePeriod, saveCB, loadCB, message, 0);
    } else {
      // Do nothing: do not init twice the same form
    }

    return this;
  }

})(jQuery);
