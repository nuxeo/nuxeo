var nuxeo = nuxeo || {};

nuxeo.documentsImport = (function(m) {

  m.createDocumentsImportDocumentHandler = function(batchId) {
    var handler = function DropZoneUIHandler(idx, dropZoneId, options, targetSelectedCB, cancelCB) {
      this.idx = idx;
      this.dropZoneId = dropZoneId;
      this.nxUploadStarted = 0;
      this.batchId = batchId;
      this.url = options.url;
      this.ctx = options.dropContext;
      this.uploadedFiles = [];
      this.targetSelectedCB = targetSelectedCB;
      this.opts = options;
      this.cancelCB = cancelCB;
    };
    handler.prototype = {
      initBatch: function(callback) {
        callback(null);
      },
      batchStarted: function() {
        jQuery("#" + this.dropZoneId).html();
        // deactivate import button
        m.disableBulkImportButton();
        this.selectTargetZone();
        return this.batchId;
      },
      batchFinished: function(batchId) {
        // activate import button
        m.enableBulkImportButton();
      },
      uploadStarted: function(fileIndex, file) {
        this.nxUploadStarted++;

        var filenameSpan = jQuery("<span />", {
          "id": "dropzone-info-" + this.idx + "-" + fileIndex,
          "class": "droppedItemInProgress"
        }).html(file.name);
        var progressSpan = jQuery("<span />", {
          "id": "dropzone-speed-" + this.idx + "-" + fileIndex,
          "class": "progressBar"
        });
        var progressContainerSpan = jQuery("<span />", {
          "class": "progressBarContainer"
        }).append(progressSpan);

        var fileDiv = jQuery("<div />", {
          "id": "dropzone-info-item-" + this.idx + "-" + fileIndex,
          "class": "simpleBox"
        }).append(filenameSpan).append(progressContainerSpan);

        var dropZone = jQuery("#" + this.dropZoneId);
        dropZone.find(".jsTips").remove();
        dropZone.append(fileDiv);
      },

      uploadFinished: function(fileIndex, file, duration) {
        var fileSpan = jQuery("<span />", {
          "class": "droppedItem"
        });
        fileSpan.html(file.name + " (" + getReadableFileSizeString(file.size) + ")");

        jQuery("#dropzone-info-item-" + this.idx + "-" + fileIndex).html(fileSpan);

        //jQuery("#dropzone-bar-msg").html(this.nxUploaded + "/" + this.nxUploadStarted);

        this.nxUploaded++;
        this.uploadedFiles.push(file);
      },
      fileUploadProgressUpdated: function(fileIndex, file, newProgress){
        jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex).css("width", newProgress + "%");
      },
      fileUploadSpeedUpdated: function(fileIndex, file, kbPerSecond) {
      },
      selectTargetZone: function() {
        var dzone = jQuery("#" + this.dropZoneId); // XXX
        dzone.addClass("dropzoneTarget");
        dzone.addClass("dropzoneFilled");
        this.targetSelectedCB(this.dropZoneId);
      },
      cancelUpload: function() {
        var dzone = jQuery("#" + this.dropZoneId);
        dzone.removeClass("dropzoneTarget");
        dzone.removeClass("dropzoneTargetExtended");
        var dragoverTimer = dzone.data("dragoverTimer");
        if (dragoverTimer) {
          window.clearTimeout(dragoverTimer);
          dzone.removeData("dragoverTimer");
        }
        this.cancelCB();
        if (this.batchId) {
          var targetUrl = this.url + 'upload/' + this.batchId;
          jQuery.ajax({
            type: 'DELETE',
            url: targetUrl,
            timeout: 10000});
        }
      },
      enableExtendedMode: function() {
        // do nothing
      }
    };

    return handler;
  };

  m.disableBulkImportButton = function() {
    jQuery(".jsDocumentsImportButton").attr("disabled", "disabled");
  };

  m.enableBulkImportButton = function() {
    var ele = jQuery(".jsDocumentsImportButton");
    if (jQuery("[data-selectedimportfolder='true']").length > 0) {
      ele.removeAttr("disabled");
      ele.removeClass("disabled");
    }
  };

  m.onClearPressed = function() {
    if (jQuery("a.rf-fu-itm-lnk").length == 0)  {
     m.disableBulkImportButton();
    }
  };

  return m

}(nuxeo.documentsImport || {}));
