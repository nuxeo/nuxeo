
//****************************
// UI Handler
// contains UI related methods and CallBacks
function DropZoneUIHandler(idx, dropZoneId, options,targetSelectedCB) {

  this.idx=idx;
  this.dropZoneId = dropZoneId;
  this.batchId=null;
  this.nxUploaded=0;
  this.nxUploadStarted=0;
  this.url=options.url;
  this.ctx=options.dropContext;
  this.operationsDef=null;
  this.uploadedFiles = new Array();
  this.targetSelectedCB = targetSelectedCB;

  DropZoneUIHandler.prototype.uploadStarted = function(fileIndex, file){
      this.nxUploadStarted++;

      jQuery("#dndMsgUploadInProgress").css("display","block");
      jQuery("#dndMsgUploadCompleted").css("display","none");

      // UI Display
      var infoDiv = jQuery("<div></div>");
      infoDiv.addClass("dropzone-info-name");
      infoDiv.attr("id", "dropzone-info-" + this.idx + "-" + fileIndex);
      infoDiv.html(file.fileName);

      var progressDiv = jQuery("<div></div>");
      progressDiv.addClass("dropzone-info-progress");
      progressDiv.attr("id", "dropzone-speed-" + this.idx + "-" + fileIndex);

      var fileDiv = jQuery("<div></div>");
      fileDiv.addClass("dropzone-info-item");
      fileDiv.attr("id", "dropzone-info-item-" + this.idx + "-" + fileIndex);
      fileDiv.append(infoDiv);
      fileDiv.append(progressDiv);

      jQuery("#dropzone-info").after(fileDiv);
  };

  DropZoneUIHandler.prototype.uploadFinished = function(fileIndex, file, duration){
      jQuery("#dropzone-info-item-" +this.idx + "-" + fileIndex).css("display","none");
      var fileDiv = jQuery("<div></div>");
      fileDiv.addClass("dropzone-info-summary-item");
      fileDiv.html(file.fileName + " ("+getReadableFileSizeString(file.fileSize)+") in " + (getReadableDurationString(duration)));
      jQuery("#dropzone-info-summary").append(fileDiv);
      this.nxUploaded++;
      this.uploadedFiles.push(file);
      jQuery("#dropzone-bar-msg").html(this.nxUploaded + "/" + this.nxUploadStarted);
  };

  DropZoneUIHandler.prototype.fileUploadProgressUpdated = function(fileIndex, file, newProgress){
      jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex).css("width", newProgress + "%");
  };

  DropZoneUIHandler.prototype.fileUploadSpeedUpdated = function(fileIndex, file, KBperSecond){
      var dive = jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex);
      dive.html( getReadableSpeedString(KBperSecond) );
  }

  DropZoneUIHandler.prototype.selectTargetZone = function () {
      var dzone = jQuery("#"+this.dropZoneId); // XXX
      dzone.addClass("dropzoneTarget");
      this.targetSelectedCB(this.dropZoneId);
  }

  DropZoneUIHandler.prototype.fetchOptions = function(){
      var handler=this; // deRef object !
      var context = jQuery("#" + this.dropZoneId).attr("context");
      // Fetch the import options
      var getOptions = jQuery().automation('ImportOptions.GET');
      getOptions.addParameter("category",context);
      getOptions.execute(function(data, textStatus,xhr) {handler.operationsDef=data;});
  }

  DropZoneUIHandler.prototype.batchStarted = function(){
      if (this.batchId==null) {
        // select the target DropZone
        this.selectTargetZone();
        // generate a batchId
        this.batchId = "batch-" + new Date().getTime();
        // fetc import options
        this.fetchOptions();
      }
      // Add the status bar on top of body
      var panel=jQuery("#dropzone-info-panel").remove();
      jQuery("body").prepend(panel);
      panel.css("display","block");
      jQuery("#dndMsgUploadInProgress").css("display","block");
      jQuery("#dndMsgUploadCompleted").css("display","none");
      jQuery("#dropzone-bar-msg").html("...");
      jQuery("#dropzone-bar-btn").css("visibility","hidden");

      return this.batchId;
  }

  DropZoneUIHandler.prototype.batchFinished = function(batchId) {
    this.showContinue(batchId);
  }

  DropZoneUIHandler.prototype.showContinue = function(batchId) {
    // Show the continue button in bar
    jQuery("#dndMsgUploadInProgress").css("display","none");
    jQuery("#dndMsgUploadCompleted").css("display","block");
    var continueButtonInBar =jQuery("#dropzone-bar-btn");
    continueButtonInBar.css("visibility","visible");
    continueButtonInBar.attr("value","Continue");

    // Show the continue button at center of dropzone
    var continueButton = jQuery("#dndContinueButton");
    continueButton.css("position","absolute");
    continueButton.css("display","block");
    continueButton.css("z-index","5000");
    var zone = jQuery("#" + this.dropZoneId);
    var btnPosition =zone.position();
    btnPosition.top = btnPosition.top + zone.height()/2 - continueButton.height()/2;
    btnPosition.left = btnPosition.left + zone.width()/2 - continueButton.width()/2;
    log(btnPosition);
    continueButton.css(btnPosition);

    // bind click
    var handler=this; // deRef object !
    continueButtonInBar.unbind();
    continueButtonInBar.bind("click",function(event) {handler.selectOperation(batchId, handler.dropZoneId, handler.url)});
    continueButton.unbind();
    continueButton.bind("click",function(event) {continueButton.css("display","none");handler.selectOperation(batchId, handler.dropZoneId, handler.url)});

  }

  DropZoneUIHandler.prototype.updateForm  = function (event, value) {
    log("updateForm : " + value);
    for (i=0; i< this.operationsDef.length; i++) {
      if(this.operationsDef[i].operationId==value) {
       var desc = jQuery("<div></div>");
       desc.html(this.operationsDef[i].description + "<br/>");
       jQuery("#dndSubForm").html(desc);
        break;
      }
    }
    var panel = jQuery("#dndFormPanel");
    var body = jQuery("body");
    var panelPosition =body.position();
    jQuery("body").append(panel);
    if (panel.width() > 0.8*body.width()) {
      panel.css("width",0.8*body.width());
    }
    panelPosition.top = panelPosition.top + body.height()/2 - panel.height()/2;
    panelPosition.left = panelPosition.left + body.width()/2 - panel.width()/2;
    panel.css(panelPosition);
  }

  DropZoneUIHandler.prototype.selectOperation  = function (batchId, dropId, url) {

    var o=this; // deRef object !
    log(this.operationsDef);
    if (this.operationsDef==null) {
      log("No OpDEf found !!!");
    } else {
      if (this.operationsDef.length==1 && this.operationsDef[0].formUrl=='') {
        // XXX start operation right now
      log("Only one operation");
      this.executeBatch(this.operationsDef[0].operationId,{});
      return;
      }
    }

    // Build the form
    log("build form");
    var panel = jQuery("#dndFormPanel");

    // update the file list
    var fileList = jQuery("#uploadedFileList");
    fileList.html("");
    for (i=0;i< this.uploadedFiles.length; i++) {
        var fileItem = jQuery("<div></div>");
        file = this.uploadedFiles[i];
        fileItem.html(file.fileName + " ("+getReadableFileSizeString(file.fileSize) + ")");
        fileList.append(fileItem);
    }

    // fill the selector
    var selector = jQuery("#operationIdSelector");
    selector.html("");
    for (i=0; i< this.operationsDef.length; i++) {
      var optionEntry = jQuery("<option></option>")
      optionEntry.attr("value",this.operationsDef[i].operationId);
      optionEntry.text(this.operationsDef[i].label);
      selector.append(optionEntry);
    }
    selector.unbind();
    selector.bind("change", function(event) {o.updateForm(event, selector.val())});
    var buttonForm = jQuery("#dndFormSubmitButton");

    panel.css("display","block");
    this.updateForm(null,this.operationsDef[0].operationId);

    buttonForm.unbind();
    buttonForm.bind("click", function(event) {
        var formParams = {};
        // gather form params
        // XXX not used for now
        jQuery("#dndSubForm>div>:input").each(function(index,element) {
           formParams[element.name] = escape(element.value);
        });
        // execute automation batch call
        var operationId = jQuery("#operationIdSelector").val();
        o.executeBatch(operationId,formParams);
    });
  }

  DropZoneUIHandler.prototype.executeBatch = function(operationId, params) {
      log("exec operation " + operationId + ", batchId=" + this.batchId);

      // hide the top panel
      jQuery("#dropzone-info-panel").css("display","none");

      // change the continue button to a loging anim
      var continueButton = jQuery("#dndContinueButton");
      continueButton.unbind();
      jQuery("#dndContinueButtonNext").css("display","none");
      jQuery("#dndContinueButtonWait").css("display","block");
      continueButton.css("display","block")

      var batchExec=jQuery().automation(operationId);
      log(this.ctx);
      batchExec.setContext(this.ctx);
      batchExec.addParameters(params);
      log(batchExec);
      batchExec.batchExecute(this.batchId,
          function(data, status,xhr) {
              log("Import operation executed OK");
              window.location.href=window.location.href;
              log("refresh-done");
          },
          function(xhr,status,e) {
              log("Error while executing batch");
          }
      );
  }

  DropZoneUIHandler.prototype.removeDropPanel = function(dropId, batchId) {
      jQuery("#dropPanel-" + this.idx ).after(jQuery("#" + dropId))
      jQuery("#dropPanel-" + this.idx).remove();
      this.batchId=null;
      this.nxUploaded=0;
      this.nxUploadStarted=0;
      alert("Batch " + batchId + " completed !!!");
  }

};
// ******************************


// ******************************
// JQuery binding

var NxDropZoneHandlerIdx=0;
var ids = new Array();
var highLightOn=false;
var targetSelected=false;

(function($) {

   $.fn.nxDropZone = function ( options ) {

     this.each(function(){
       var dropId = jQuery(this).attr("id");
       ids.push(dropId);
       log("Init handler for " + dropId)
       // create UI handler
       var uiHandler = new DropZoneUIHandler(NxDropZoneHandlerIdx++, dropId, options, function(targetId) {
                                                 targetSelected=true;
                                                 jQuery.each(ids, function (idx,id) {
                                                   if (id!=targetId) {
                                                     jQuery("#"+id).unbind();
                                                   }
                                                   jQuery("#"+id).unbind('dragleave');
                                                 });
       });
       // copy optionMap
       var instanceOptions = jQuery.extend({},options);
       // register callback Handler
       instanceOptions.handler = uiHandler;
       jQuery("#" + dropId).dropzone(instanceOptions);
       log("Init " + dropId + " done!")
     })

      // bind events on body to show the drop box
      document.body.ondragover = function(event) {highlightDropZones(event)};
      document.body.ondragleave = function(event) {removeHighlights(event)};
     };

     function highlightDropZones(event) {

         if (highLightOn || targetSelected ) {
           return;
         }

          var dt = event.dataTransfer;
          if (!dt && event.originalEvent) {
            // JQuery event wrapper
            dt = event.originalEvent.dataTransfer;
          }
          if (dt) {
            if ("Files"==dt.types // chrome
              || dt.types.length==0
              || "application/x-moz-file"== dt.types[0] // firefox
            ) {
              jQuery.each(ids, function (idx,id) {
                jQuery("#"+id).addClass("dropzoneHL");
              });
              highLightOn = true;
               } else {
                 //log("no data");
                 //log(dt);
               }
          } else {
              //log("No dataTransfer");
          }
      };

      function removeHighlights(event) {
          if (!highLightOn || targetSelected ) {
              return;
          }
          //log(event);
          jQuery.each(ids, function (idx,id) {
            jQuery("#"+id).removeClass("dropzoneHL");
//          if(!event.relatedTarget || event.relatedTarget.id!=id) {
//            jQuery("#"+id).removeClass("dropzoneHL")
//          }
          });
          highLightOn = false;

      }

 })(jQuery);
