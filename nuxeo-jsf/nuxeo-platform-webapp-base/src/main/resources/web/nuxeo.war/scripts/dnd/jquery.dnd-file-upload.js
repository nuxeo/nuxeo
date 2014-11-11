//********************************************************
// JQuery wrapper to manage File upload via Drag and Drop
//
// Base code comes from https://github.com/pangratz/dnd-file-upload/network
//

(function($) {

  var sendingRequestsInProgress=false;
  var uploadStack = new Array();
  var uploadIdx=0;
  var nbUploadInprogress=0;
  var completedUploads = new Array();

  $.fn.dropzone = function(options) {

    // Extend our default options with those provided.
    var opts = jQuery.extend( {}, $.fn.dropzone.defaults, options);
    this.data("opts", opts);

    var id = this.attr("id");
    var dropzone = document.getElementById(id);

    if (window.$.client.browser == "Safari" && window.$.client.os == "Windows") {
      var fileInput = jQuery("<input>");
      fileInput.attr( {
        type : "file"
      });
      fileInput.bind("change", function(event) { change(event,opts)});
      fileInput.css( {'opacity' : '0', 'width' : '100%','height' : '100%'});
      fileInput.attr("multiple", "multiple");
      fileInput.click(function() {
        return false;
      });
      this.append(fileInput);
    } else {
      dropzone.addEventListener("drop", function(event) { drop(event,opts);}, true);
      var jQueryDropzone = jQuery("#" + id);
      jQueryDropzone.bind("dragenter",  function(event) {dragenter(event,jQueryDropzone, opts);});
      jQueryDropzone.bind("dragover", dragover);
    }

    return this;
  };

  $.fn.dropzone.defaults = {
    url : "",
    method : "POST",
    numConcurrentUploads : 5,
    // define if upload should be triggered directly
    directUpload : true,
    // update upload speed every second
    uploadRateRefreshTime : 1000,
    // time to enable extended mode
    extendedModeTimeout : 1500,
    // http requests timeout
    uploadTimeout : 30000,
    execTimeout : 30000,
    handler : {
      // invoked when new files are dropped
      batchStarted : function() {return "X"},
      // invoked when the upload for given file has been started
      uploadStarted : function(fileIndex, file) {},
      // invoked when the upload for given file has been finished
      uploadFinished : function(fileIndex, file, time) {},
      // invoked when the progress for given file has changed
      fileUploadProgressUpdated : function(fileIndex, file,newProgress) {},
      // invoked when the upload speed of given file has changed
      fileUploadSpeedUpdated : function(fileIndex, file,KBperSecond) {},
      // invoked when all files have been uploaded
      batchFinished : function(batchId) {},
      // invoked to enable Extended mode
      enableExtendedMode : function(id) {console.log('Enable extended mode for zone ' + id )}
   }
  };

  function dragenter(event,zone,opts) {

    var id = zone.attr('id');

    log("dragenter on zone " + id);
    zone.addClass("dropzoneTarget");
    event.stopPropagation();
    event.preventDefault();

    var dragoverTimer = zone.data("dragoverTimer");
    if (!dragoverTimer && opts.extendedModeTimeout>0) {
      dragoverTimer = window.setTimeout(function() {opts.handler.enableExtendedMode(id);}, opts.extendedModeTimeout);
      zone.data("dragoverTimer", dragoverTimer);
    }
    applyOverlay(zone,opts);
    return false;
  }

  function dragleave(event,id) {
     var zone =jQuery("#"+id);
     zone.removeClass("dropzoneTarget");
     var dragoverTimer = zone.data("dragoverTimer");
     if (dragoverTimer) {
       window.clearTimeout(dragoverTimer);
       zone.removeData("dragoverTimer");
     }
     return false;
  }

  function dragover(event) {
    event.stopPropagation();
    event.preventDefault();
    return false;
  }

  function drop(event, opts) {

    event.preventDefault();
    var files = event.dataTransfer.files;
    for ( var i = 0; i < files.length; i++) {
      uploadStack.push(files[i]);
    }

    if (opts.directUpload && !sendingRequestsInProgress && uploadStack.length>0) {
      uploadFiles(opts);
    }
    return false;
  }

  function applyOverlay(zone,opts) {
      log("apply Overlay on zone " + zone.attr("id"));
      zone.addClass("dropzoneTarget");
      if (jQuery.browser.mozilla && jQuery.browser.version.startsWith("1.")) {
        // overlay does break drop event catching in FF 3.6 !!!
        zone.bind("dragleave",  function(event) {removeOverlay(event, null, zone, opts);});
      } else {
        // Webkit and FF4 => use Overlay
        var overlay = jQuery("<div></div>");
        overlay.addClass("dropzoneTargetOverlay");
        overlay.css(zone.position());
        overlay.width(zone.width()+2);
        overlay.height(zone.height()+2);
        zone.append(overlay);
        overlay.bind("dragleave",  function(event) { removeOverlay(event, overlay, zone, opts);});
        zone.unbind("dragenter");
        log("overlay applied");
      }
   }

  function removeOverlay(event,overlay,zone,opts) {
       zone.removeClass("dropzoneTarget");
       if (overlay!=null) {
         overlay.unbind();
         overlay.css("display","none");
         overlay.remove();
         window.setTimeout(function(){zone.bind("dragenter",  function(event) {dragenter(event,zone,opts);});},100);
       }
       var dragoverTimer = zone.data("dragoverTimer");
       if (dragoverTimer) {
         window.clearTimeout(dragoverTimer);
         zone.removeData("dragoverTimer");
       }
       return false;
   }

  function log(logMsg) {
      //console && console.log(logMsg);
  }

  function uploadFiles(opts) {
    var batchId = opts.handler.batchStarted();
    sendingRequestsInProgress=true;

    while (uploadStack.length>0) {
      var file = uploadStack.shift();
      // create a new xhr object
      var xhr = new XMLHttpRequest();
      var upload = xhr.upload;
      upload.fileIndex = uploadIdx+0;
      upload.fileObj = file;
      upload.downloadStartTime = new Date().getTime();
      upload.currentStart = upload.downloadStartTime;
      upload.currentProgress = 0;
      upload.startData = 0;
      upload.batchId = batchId;

      // add listeners
      upload.addEventListener("progress", function(event) {progress(event,opts)}, false);

      // The "load" event doesn't work correctly on WebKit (Chrome, Safari),
      // it fires too early, before the server has returned its response.
      // still it is required for Firefox
      if (navigator.userAgent.indexOf('Firefox') > -1) {
        upload.addEventListener("load", function(event) {log("trigger load"); log(event); load(event.target,opts)}, false);
      }

      // on ready state change is not fired in all cases on webkit
      // - on webkit we rely on progress lister to detected upload end
      // - but on Firefox the event we need it
      xhr.onreadystatechange = function() {readyStateChange(xhr, opts)};

      // propagate callback
      upload.uploadFiles = uploadFiles;

      // compute timeout in seconds and integer
      uploadTimeoutS = 5+(opts.uploadTimeout/1000)|0;

      var targetUrl = opts.url;
      if (targetUrl.indexOf("/", targetUrl.length - 1)==-1) {
        targetUrl = targetUrl + "/";
      }
      targetUrl =  targetUrl + "batch/upload";

      log("starting upload for file " + uploadIdx);
      xhr.open(opts.method, targetUrl);
      xhr.setRequestHeader("Cache-Control", "no-cache");
      xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
      xhr.setRequestHeader("X-File-Name", encodeURIComponent(file.name));
      xhr.setRequestHeader("X-File-Size", file.size);
      xhr.setRequestHeader("X-File-Type", file.type);
      xhr.setRequestHeader("X-Batch-Id", batchId);
      xhr.setRequestHeader("X-File-Idx", uploadIdx);

      xhr.setRequestHeader('Nuxeo-Transaction-Timeout', uploadTimeoutS);
      xhr.setRequestHeader("Content-Type", "multipart/form-data");
      nbUploadInprogress++;

      opts.handler.uploadStarted(uploadIdx, file);
      uploadIdx++;

      xhr.send(file);

      if (nbUploadInprogress>=opts.numConcurrentUploads) {
        sendingRequestsInProgress=false;
        log("delaying upload for next file(s) " + uploadIdx + "+ since there are already " + nbUploadInprogress + " active uploads");
        return;
      }
    }
    sendingRequestsInProgress=false;
  }

  function readyStateChange(xhr, opts) {
    var upload = xhr.upload;
    log("readyStateChange event on file upload " + upload.fileIndex + " (state : " + xhr.readyState + ")");
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {
        load(upload, opts);
      } else {
        log("Upload failed, status: " + xhr.status);
      }
    }
  }

  function load(upload, opts) {
    var fileIdx = upload.fileIndex;
    log("Received loaded event on  file " + fileIdx);
    if (completedUploads.indexOf(fileIdx)<0) {
      completedUploads.push(fileIdx);
    } else {
       log("Event already processsed for file " + fileIdx + ", exiting");
       return;
    }
    var now = new Date().getTime();
    var timeDiff = now - upload.downloadStartTime;
    opts.handler.uploadFinished(upload.fileIndex, upload.fileObj, timeDiff);
    log("upload of file " + upload.fileIndex + " completed");
    nbUploadInprogress--;
    if (!sendingRequestsInProgress && uploadStack.length>0) {
      // restart upload
      log("restart pending uploads");
      upload.uploadFiles(opts)
    }
    else if (nbUploadInprogress==0) {
      opts.handler.batchFinished(upload.batchId);
    }
  }

  function progress(event, opts) {
    log(event);
    if (event.lengthComputable) {
      var percentage = Math.round((event.loaded * 100) / event.total);
      if (event.target.currentProgress != percentage) {

        log("progress event on upload of file " + event.target.fileIndex + " --> " + percentage + "%");

        event.target.currentProgress = percentage;
        opts.handler.fileUploadProgressUpdated(event.target.fileIndex, event.target.fileObj, event.target.currentProgress);

        var elapsed = new Date().getTime();
        var diffTime = elapsed - event.target.currentStart;
        if (diffTime >= opts.handler.uploadRateRefreshTime) {
          var diffData = event.loaded - event.target.startData;
          var speed = diffData / diffTime; // in KB/sec

          opts.handler.fileUploadSpeedUpdated(event.target.fileIndex, event.target.fileObj, speed);

          event.target.startData = event.loaded;
          event.target.currentStart = elapsed;
        }
      if (event.loaded == event.total) {
        log("file " + event.target.fileIndex + " detected upload complete");
        load(event.target, opts);
      } else {
        log("file " + event.target.fileIndex + " not completed :" + event.loaded + "/" + event.total);
      }
      }
    }
  }

  // invoked when the input field has changed and new files have been dropped
  // or selected
  function change(event,opts) {
    event.preventDefault();

    // get all files ...
    var files = event.target.files;

    for ( var i = 0; i < files.length; i++) {
        uploadStack.push(files[i]);
      }
    // ... and upload them
    uploadFiles(opts);
  }

})(jQuery);
