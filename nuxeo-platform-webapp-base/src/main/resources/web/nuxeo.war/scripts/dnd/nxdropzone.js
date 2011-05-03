// ********************************************************
// Provides a JQuery wrapper to initialize Drop zones
// JQuery dropzone object is linked to a UIControler object
// that manages dialog

// some helper functcions

function log(msg) {
  if (window.console) {
    //console.log(msg);
  }
}

function getReadableSpeedString(speedInKBytesPerSec)
{
  var speed = speedInKBytesPerSec;
  speed = Math.round(speed * 10) / 10;
  if (speed < 1024) {
    return speed + "KB/s";
  }

  speed /= 1024;
  speed = Math.round(speed * 10) / 10;
  if (speed < 1024) {
    return speed + "MB/s";
  }

  return speedInBytesPerSec + "B/s";
}

function getReadableFileSizeString(fileSizeInBytes)
{
  var fileSize = fileSizeInBytes;
  if (fileSize < 1024) {
    return fileSize + "B";
  }

  fileSize /= 1024;
  fileSize = Math.round(fileSize);
  if (fileSize < 1024) {
    return fileSize + "KB";
  }

  fileSize /= 1024;
  fileSize = Math.round(fileSize * 10) / 10;
  if (fileSize < 1024) {
    return fileSize + "MB";
  }

  return fileSizeInBytes + "B";
}

function getReadableDurationString(duration)
{
  var elapsed = duration;

  var minutes, seconds;

  seconds = Math.floor(elapsed / 1000);
  minutes = Math.floor((seconds / 60));
  seconds = seconds - (minutes * 60);

  var str = "";
  if (minutes>0)
    str += minutes + "m";

  str += seconds + "s";
  return str;
}

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
  this.cancelled=false;
  this.extendedMode=false;
  this.executionPending=false;


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
      log("Fetching options");
      var handler=this; // deRef object !
      var context = jQuery("#" + this.dropZoneId).attr("context");
      // Fetch the import options
      var getOptions;
      if (this.ctx.conversationId) {
        getOptions = jQuery().automation('Chain.SeamActions.GET');
      } else {
        getOptions = jQuery().automation('Actions.GET');
      }
      getOptions.addParameter("category",context);
      getOptions.setContext(this.ctx);
      getOptions.execute(function(data, textStatus,xhr) {
                            handler.operationsDef=data;
                            if (data.length==0) {
                              handler.canNotUpload(false);
                            };
                            if(handler.executionPending) {
                              // execution was waiting for the op definitions
                              handler.executionPending=false;
                              handler.selectOperation(handler.batchId, handler.dropZoneId,handler.url);
                            }
                          },
                          function(xhr, status, e) {
                              handler.canNotUpload(true);
                          });
  }

  DropZoneUIHandler.prototype.canNotUpload = function(isError) {
    this.cancelUpload();
    if (isError) {
      alert("Upload can be continue because of an error");
    } else {
      alert("You can not upload here (may be insufisant rights)");
    }
  }

  DropZoneUIHandler.prototype.cancelUpload = function() {
    this.cancelled=true;
    jQuery("#dndContinueButton").css("display","none");
    jQuery("#dropzone-info-panel").css("display","none");
    var dzone = jQuery("#"+this.dropZoneId);
    dzone.removeClass("dropzoneTarget");

    var targetUrl = this.url + 'drop/' + this.batchId;
    jQuery.ajax({
        type: 'GET',
        contentType : 'application/json+nxrequest',
        url: targetUrl,
        timeout: 10000});
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
    if (!this.cancelled) {
      if (this.extendedMode) {
        this.showContinue(batchId);
      } else {
        this.selectOperation(this.batchId, this.dropZoneId, this.url);
      }
    }
  }

  DropZoneUIHandler.prototype.positionContinueButton = function(batchId) {
      // Show the continue button at center of dropzone
      var continueButton = jQuery("#dndContinueButton");
      continueButton.css("position","absolute");
      continueButton.css("display","block");
      continueButton.css("z-index","5000");
      var zone = jQuery("#" + this.dropZoneId);
      var btnPosition =zone.position();
      btnPosition.top = btnPosition.top + zone.height()/2 - continueButton.height()/2;
      btnPosition.left = btnPosition.left + zone.width()/2 - continueButton.width()/2;
      continueButton.css(btnPosition);
      return continueButton
  }


  DropZoneUIHandler.prototype.showContinue = function(batchId) {
    // Show the continue button in bar
    jQuery("#dndMsgUploadInProgress").css("display","none");
    jQuery("#dndMsgUploadCompleted").css("display","block");
    var continueButtonInBar =jQuery("#dropzone-bar-btn");
    continueButtonInBar.css("visibility","visible");
    continueButtonInBar.attr("value","Continue");

    // Show the continue button at center of dropzone
    var continueButton = this.positionContinueButton();

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
      if(this.operationsDef[i].id==value) {
       var desc = jQuery("<div></div>");
       desc.html(this.operationsDef[i].help + "<br/>");
       jQuery("#dndSubForm").html(desc);
       if (this.operationsDef[i].link!='') {
         jQuery("#dndFormSubmitButton").css("display","none");
         var iframe = jQuery("<iframe></iframe>");
         iframe.attr("width", "330px");
         iframe.attr("height", "400px");
         iframe.attr("frameborder", "0");
         iframe.attr("src", this.operationsDef[i].link);
         desc.append(iframe);
         var handler = this;
         window.dndFormFunctionCB=function(fData) {handler.executeBatch(value, fData);};
       } else {
         jQuery("#dndFormSubmitButton").css("display","block");
       }
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
    if (panelPosition.top<10) {
      panelPosition.top=10;
    }
    if (panelPosition.left<10) {
      panelPosition.left=10;
    }

    panel.css(panelPosition);
  }

  DropZoneUIHandler.prototype.selectOperation  = function (batchId, dropId, url) {

    jQuery("#dropzone-info-panel").css("display","none");

    var o=this; // deRef object !
    log(this.operationsDef);
    if (this.operationsDef==null) {
      this.executionPending=true;
      log("No OpDEf found !!!");
    } else {
      if ((this.extendedMode==false || this.operationsDef.length==1) && this.operationsDef[0].link=='') {
        this.executeBatch(this.operationsDef[0].id,{});
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
      optionEntry.attr("value",this.operationsDef[i].id);
      optionEntry.text(this.operationsDef[i].label);
      selector.append(optionEntry);
    }
    selector.unbind();
    selector.bind("change", function(event) {o.updateForm(event, selector.val())});
    var buttonForm = jQuery("#dndFormSubmitButton");

    panel.css("display","block");
    this.updateForm(null,this.operationsDef[0].id);

    buttonForm.unbind();
    buttonForm.bind("click", function(event) {
        // execute automation batch call
        var operationId = jQuery("#operationIdSelector").val();
        o.executeBatch(operationId,{});
    });
  }

  DropZoneUIHandler.prototype.executeBatch = function(operationId, params) {
      log("exec operation " + operationId + ", batchId=" + this.batchId);

      // hide the top panel
      jQuery("#dropzone-info-panel").css("display","none");
      jQuery("#dndFormPanel").css("display","none");

      // change the continue button to a loading anim
      var continueButton = this.positionContinueButton();
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

  DropZoneUIHandler.prototype.enableExtendedMode = function(dropId) {
    this.extendedMode=true;
    jQuery("#"+dropId).css("border-color","red");
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
          jQuery.each(ids, function (idx,id) {
            jQuery("#"+id).removeClass("dropzoneHL");
//          if(!event.relatedTarget || event.relatedTarget.id!=id) {
//            jQuery("#"+id).removeClass("dropzoneHL")
//          }
          });
          highLightOn = false;

      }

 })(jQuery);
