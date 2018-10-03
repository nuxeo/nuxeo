//********************************************************
// JQuery wrapper to manage File upload via Drag and Drop
//
// Base code comes from https://github.com/pangratz/dnd-file-upload/network
//

if (!log) {
  log = function (logMsg) {
    // console && console.log(logMsg);
  }
}

(function ($) {

  var initializingBatch = false;
  var batchInitialized = false;
  var sendingRequestsInProgress = false;
  var tryToUploadDirectoryContent = false;
  var uploadStack = new Array();
  var uploadIdx = 0;
  var nbDropFilesToProcess = 0;
  var nbUploadInprogress = 0;
  var completedUploads = new Array();
  var currentDropZone;


  $.fn.dropzone = function (options, loadAlreadyUploadedFiles) {

    // Extend our default options with those provided.
    var opts = jQuery.extend({}, $.fn.dropzone.defaults, options);
    this.data("opts", opts);

    var id = this.attr("id");
    var dropzone = document.getElementById(id);

    if (window.$.client.browser == "Safari" && window.$.client.os == "Windows") {
      var fileInput = jQuery("<input>");
      fileInput.attr({
        type: "file"
      });
      fileInput.bind("change", function (event) {
        change(event, opts)
      });
      fileInput.css({'opacity': '0', 'width': '100%', 'height': '100%'});
      fileInput.attr("multiple", "multiple");
      fileInput.click(function () {
        return false;
      });
      this.append(fileInput);
    } else {
      dropzone.addEventListener("drop", function (event) {
        drop(event, opts);
      }, true);
      var jQueryDropzone = jQuery("#" + id);
      jQueryDropzone.bind("dragenter", function (event) {
        dragenter(event, jQueryDropzone, opts);
      });
      jQueryDropzone.bind("dragover", dragover);
    }

    // load already uploaded files for this batch
    if (loadAlreadyUploadedFiles) {
      var targetUrl = opts.url;
      if (targetUrl.indexOf("/", targetUrl.length - 1) == -1) {
        targetUrl = targetUrl + "/";
      }
      targetUrl = targetUrl + "upload/" + opts.handler.batchStarted();
      jQuery.ajax({
        type: 'GET',
        contentType: 'application/json',
        url: targetUrl,
        timeout: 10000
      }).done(function (data, textStatus, xhr) {
        if (xhr.status === 200) {
          for (var i = 0, len = data.length; i < len; i++) {
            opts.handler.uploadStarted(i, data[i]);
            opts.handler.uploadFinished(i, data[i], null);
          }
          uploadIdx = data.length;
        }
      });
    }

    return this;
  };

  $.fn.dropzone.defaults = {
    url: "",
    method: "POST",
    numConcurrentUploads: 5,
    // define if upload should be triggered directly
    directUpload: true,
    // update upload speed every second
    uploadRateRefreshTime: 1000,
    // time to enable extended mode
    extendedModeTimeout: 1500,
    // http requests timeout
    uploadTimeout: 30000,
    execTimeout: 30000,
    handler: {
      // invoked to generate a batchId server-side
      initBatch: function(callback) {
        callback(null);
      },
      // invoked when new files are dropped
      batchStarted: function () {
        return "X";
      },
      // invoked when the upload for given file has been started
      uploadStarted: function (fileIndex, file) {
      },
      // invoked when the upload for given file has been finished
      uploadFinished: function (fileIndex, file, time) {
      },
      // invoked when the progress for given file has changed
      fileUploadProgressUpdated: function (fileIndex, file, newProgress) {
      },
      // invoked when the upload speed of given file has changed
      fileUploadSpeedUpdated: function (fileIndex, file, KBperSecond) {
      },
      // invoked when all files have been uploaded
      batchFinished: function (batchId) {
      },
      // invoked to enable Extended mode
      enableExtendedMode: function (id) {
        console.log('Enable extended mode for zone ' + id)
      }
    }
  };

  function dragenter(event, zone, opts) {

    var id = zone.attr('id');

    log("dragenter on zone " + id);
    zone.addClass("dropzoneTarget");
    event.stopPropagation();
    event.preventDefault();

    var dragoverTimer = zone.data("dragoverTimer");
    if (!dragoverTimer && opts.extendedModeTimeout > 0) {
      dragoverTimer = window.setTimeout(function () {
        opts.handler.enableExtendedMode(id);
      }, opts.extendedModeTimeout);
      zone.data("dragoverTimer", dragoverTimer);
    }
    currentDropZone = zone;
    applyOverlay(zone, opts);
    return false;
  }

  function dragleave(event, id) {
    var zone = jQuery("#" + id);
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

  function getDirectoryEntries(directoryReader, opts) {
    nbDropFilesToProcess += 1;
    var readEntries = function () {
      directoryReader.readEntries(function (results) {
        if (!results.length) {
          processFileEntry(null, opts);
        } else {
          nbDropFilesToProcess += (results || []).length - 1;
          for (var i = 0; i < (results || []).length; i++) {
            if (results[i].isDirectory) {
              getDirectoryEntries(results[i].createReader(), opts);
            } else {
              results[i].file(function (f) {
                processFileEntry(f, opts);
              }, function (e) {
                log(e);
              });
            }
          }
          readEntries();
        }
      }, function (e) {
        log(e)
      });
    };
    readEntries();
  }

  function processFileOrFolderEntryAsync(fileOb, opts, fileCB, folderCB, transferItem) {
    // filter Folders from dropped content
    // Folder size is 0 under Win / %4096 under Linux / variable under MacOS
    if (!fileOb.type && fileOb.size < (4096 * 4 + 1)) {
      try {
        // need to test the file by reading it ...
        var reader = new FileReader();
        reader.onerror = function (e) {
          folderCB(fileOb, opts, transferItem);
        };
        reader.onabort = function (e) {
          folderCB(fileOb, opts, transferItem);
        };
        reader.onload = function (e) {
          fileCB(fileOb, opts);
        };
        reader.readAsText(fileOb);
      } catch (err) {
        folderCB(fileOb, opts, transferItem);
      }
    } else {
      fileCB(fileOb, opts);
    }
  }

  function cancelUploadIfNoValidFileWasDropped(opts) {
    if (uploadStack.length == 0 && uploadIdx == 0 && nbDropFilesToProcess == 0) {
      opts.handler.cancelUpload();
      if (currentDropZone) {
        var zone = jQuery("#" + currentDropZone.attr("id"));
        currentDropZone = null;
        dragleave(null, zone.attr("id"));
        zone.removeClass("dropzoneTarget");
        zone.bind("dragenter", function (zone, opts) {
          return function (event) {
            dragenter(event, zone, opts);
          }
        }(zone, opts));
      }
    }
  }

  function processFileEntry(cfile, opts) {
    nbDropFilesToProcess--;
    if (cfile) {
      uploadStack.push(cfile);
    }
    if (opts.directUpload && !sendingRequestsInProgress && uploadStack.length > 0) {
      uploadFiles(opts);
    } else {
      cancelUploadIfNoValidFileWasDropped(opts);
    }
  }

  function processFolderEntry(cfolder, opts, folderEntry) {
    if (tryToUploadDirectoryContent && folderEntry) {
      var directoryReader = folderEntry.createReader();
      if (directoryReader) {
        getDirectoryEntries(directoryReader, opts);
      }
    } else {
      nbDropFilesToProcess--;
      log("skipping folder");
    }
    cancelUploadIfNoValidFileWasDropped(opts);
  }

  function drop(event, opts) {
    event.preventDefault();
    var files = event.dataTransfer.files;
    nbDropFilesToProcess += files.length;
    for (var i = 0; i < files.length; i++) {
      var cfile = files[i];
      var folderEntry;
      if (event.dataTransfer.items && event.dataTransfer.items[i].webkitGetAsEntry) {
        folderEntry = event.dataTransfer.items[i].webkitGetAsEntry();
      }
      processFileOrFolderEntryAsync(cfile, opts, processFileEntry, processFolderEntry, folderEntry);
    }
    return false;
  }

  function applyOverlay(zone, opts) {
    log("apply Overlay on zone " + zone.attr("id"));
    zone.addClass("dropzoneTarget");
    if (jQuery.browser.mozilla && jQuery.browser.version.indexOf("1.") === 0) {
      // overlay does break drop event catching in FF 3.6 !!!
      zone.bind("dragleave", function (event) {
        removeOverlay(event, null, zone, opts);
      });
    } else {
      // Webkit and FF4 => use Overlay
      var overlay = jQuery("<div></div>");
      overlay.addClass("dropzoneTargetOverlay");
      zone.append(overlay);
      resizeOverlay(zone);
      overlay.bind("dragleave", function (event) {
        removeOverlay(event, overlay, zone, opts);
      });
      zone.unbind("dragenter");
      log("overlay applied");
    }
  }

  function resizeOverlay(dropZone) {
    dropZone = jQuery(dropZone)
    dropZone.find(".dropzoneTargetOverlay").each(function () {
      var overlay = jQuery(this);
      overlay.css(dropZone.position());
      var computedWidth = dropZone.width();
      computedWidth += parseInt(dropZone.css('padding-right'), 10);
      computedWidth += parseInt(dropZone.css('padding-left'), 10);
      computedWidth += parseInt(dropZone.css('margin-right'), 10);
      computedWidth += parseInt(dropZone.css('margin-left'), 10);
      overlay.width(computedWidth);

      var computedHeight = dropZone.height();
      computedHeight += parseInt(dropZone.css('padding-top'), 10);
      computedHeight += parseInt(dropZone.css('padding-bottom'), 10);
      computedHeight += parseInt(dropZone.css('margin-top'), 10);
      computedHeight += parseInt(dropZone.css('margin-bottom'), 10);
      overlay.height(computedHeight);
    })
  }

  function removeOverlay(event, overlay, zone, opts) {
    zone.removeClass("dropzoneTarget");
    if (overlay != null) {
      overlay.unbind();
      overlay.css("display", "none");
      overlay.remove();
      window.setTimeout(function () {
        zone.bind("dragenter", function (event) {
          dragenter(event, zone, opts);
        });
      }, 100);
      currentDropZone = null;
    }
    var dragoverTimer = zone.data("dragoverTimer");
    if (dragoverTimer) {
      window.clearTimeout(dragoverTimer);
      zone.removeData("dragoverTimer");
    }
    return false;
  }

  function uploadFiles(opts) {
    if (nbUploadInprogress >= opts.numConcurrentUploads) {
      sendingRequestsInProgress = false;
      log("delaying upload for next file(s) " + uploadIdx + "+ since there are already " + nbUploadInprogress + " active uploads");
      return;
    }

    if (!batchInitialized && !initializingBatch) {
      initializingBatch = true;
      opts.handler.initBatch(function(err) {
        if (err) {
          throw new Error(err);
        }

        batchInitialized = true;
        doUploadFiles(opts);
      });
    } else if (batchInitialized) {
      doUploadFiles(opts);
    }
  }

  function doUploadFiles(opts) {
    var batchId = opts.handler.batchStarted();

    sendingRequestsInProgress = true;
    while (uploadStack.length > 0) {
      var file = uploadStack.shift();
      // create a new xhr object
      var xhr = new XMLHttpRequest();
      var upload = xhr.upload;
      upload.fileIndex = uploadIdx + 0;
      upload.fileObj = file;
      upload.downloadStartTime = new Date().getTime();
      upload.currentStart = upload.downloadStartTime;
      upload.currentProgress = 0;
      upload.startData = 0;
      upload.batchId = batchId;

      // add listeners
      upload.addEventListener("progress", function (event) {
        progress(event, opts)
      }, false);

      // The "load" event doesn't work correctly on WebKit (Chrome, Safari),
      // it fires too early, before the server has returned its response.
      // still it is required for Firefox
      if (navigator.userAgent.indexOf('Firefox') > -1) {
        upload.addEventListener("load", function (event) {
          log("trigger load");
          log(event);
          load(event.target, opts)
        }, false);
      }

      // on ready state change is not fired in all cases on webkit
      // - on webkit we rely on progress lister to detected upload end
      // - but on Firefox the event we need it
      xhr.onreadystatechange = (function (xhr, opts) {
        return function () {
          readyStateChange(xhr, opts)
        }
      })(xhr, opts);

      // propagate callback
      upload.uploadFiles = uploadFiles;

      // compute timeout in seconds and integer
      uploadTimeoutS = 5 + (opts.uploadTimeout / 1000) | 0;

      var targetUrl = opts.url;
      if (targetUrl.indexOf("/", targetUrl.length - 1) == -1) {
        targetUrl = targetUrl + "/";
      }
      targetUrl = targetUrl + "upload/" + batchId + "/" + uploadIdx;

      log("starting upload for file " + uploadIdx);
      xhr.open(opts.method, targetUrl);
      xhr.setRequestHeader("Cache-Control", "no-cache");
      xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
      xhr.setRequestHeader("X-File-Name", encodeURIComponent(file.name));
      xhr.setRequestHeader("X-File-Size", file.size);
      xhr.setRequestHeader("X-File-Type", file.type);

      xhr.setRequestHeader('Nuxeo-Transaction-Timeout', uploadTimeoutS);
      nbUploadInprogress++;

      opts.handler.uploadStarted(uploadIdx, file);
      uploadIdx++;

      // resize the overlay
      jQuery(".dropzoneTarget").each(function () {
        resizeOverlay(this);
      });

      xhr.send(file);

      if (nbUploadInprogress >= opts.numConcurrentUploads) {
        sendingRequestsInProgress = false;
        log("delaying upload for next file(s) " + uploadIdx + "+ since there are already " + nbUploadInprogress + " active uploads");
        return;
      }
    }
    sendingRequestsInProgress = false;
  }

  function readyStateChange(xhr, opts) {
    var upload = xhr.upload;
    log("readyStateChange event on file upload " + upload.fileIndex + " (state : " + xhr.readyState + ")");
    if (xhr.readyState == 4) {
      if (xhr.status == 201) {
        load(upload, opts);
      } else {
        log("Upload failed, status: " + xhr.status);
      }
    }
  }

  function load(upload, opts) {
    var fileIdx = upload.fileIndex;
    log("Received loaded event on  file " + fileIdx);
    if (completedUploads.indexOf(fileIdx) < 0) {
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
    if (!sendingRequestsInProgress && uploadStack.length > 0 && nbUploadInprogress < opts.numConcurrentUploads) {
      // restart upload
      log("restart pending uploads");
      upload.uploadFiles(opts);
    }
    else if (nbUploadInprogress == 0) {
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
          // having all the bytes sent to the server does not mean the server did actually receive everything
          // but since load event is not reliable on Webkit we need this
          // window.setTimeout(function(){load(event.target, opts);}, 5000);
        } else {
          log("file " + event.target.fileIndex + " not completed :" + event.loaded + "/" + event.total);
        }
      }
    }
  }

  // invoked when the input field has changed and new files have been dropped
  // or selected
  function change(event, opts) {
    event.preventDefault();

    // get all files ...
    var files = event.target.files;

    for (var i = 0; i < files.length; i++) {
      uploadStack.push(files[i]);
    }
    // ... and upload them
    uploadFiles(opts);
  }

})(jQuery);
