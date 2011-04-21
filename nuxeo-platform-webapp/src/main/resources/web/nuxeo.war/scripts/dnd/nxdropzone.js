
//****************************
// UI Handler
// contains UI related methods and CallBacks
function DropZoneUIHandler(idx, dropZoneId, url) {

  this.idx=idx;
  this.dropZoneId = dropZoneId;
  this.batchId=null;
  this.nxUploaded=0;
  this.nxUploadStarted=0;
  this.url=url;
  this.operationsDef=null;

  DropZoneUIHandler.prototype.uploadStarted = function(fileIndex, file){
      this.nxUploadStarted++;

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
      fileDiv.append(infoDiv);
      fileDiv.append(progressDiv);

      jQuery("#dropzone-info-" + this.idx).after(fileDiv);
  };

  DropZoneUIHandler.prototype.uploadFinished = function(fileIndex, file, duration){
      jQuery("#dropzone-info-" +this.idx + "-" + fileIndex).html(file.fileName + " ("+getReadableFileSizeString(file.fileSize)+") in " + (getReadableDurationString(duration)));
      jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex).css({
                'width': '100%',
                'background-color': 'green'
              });

      this.nxUploaded++;
      jQuery("#dropzone-bar-msg-" + this.idx).html("import in progress :" + this.nxUploaded + "/" + this.nxUploadStarted);

  };

  DropZoneUIHandler.prototype.fileUploadProgressUpdated = function(fileIndex, file, newProgress){
      jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex).css("width", newProgress + "%");
  };

  DropZoneUIHandler.prototype.fileUploadSpeedUpdated = function(fileIndex, file, KBperSecond){
      var dive = jQuery("#dropzone-speed-" + this.idx + "-" + fileIndex);
      dive.html( getReadableSpeedString(KBperSecond) );
  }

  DropZoneUIHandler.prototype.buildUI = function () {
      var dzone = jQuery("#"+this.dropZoneId); // XXX

      var panel = jQuery("<div></div>");
      panel.addClass("dropPanel");
      panel.attr("id", "dropPanel-" + this.idx);

      dzone.before(panel);
      panel.append(dzone);

      var infoBar = jQuery("<div></div>");
      infoBar.addClass("dropzone-bar");
      infoBar.attr("id", "dropzone-bar-" + this.idx);
      infoBar.html("<span id=\"dropzone-bar-msg-" + this.idx + "\" class=\"dropzone-bar-msg\"></span><input id=\"dropzone-bar-btn-" + this.idx + "\" class=\"dropzone-bar-btn\" type=\"button\" value=\"Continue\">");

      var infoBlock = jQuery("<div></div>");
      infoBlock.addClass("dropzone-info-block");
      infoBlock.attr("id", "dropzone-info-block-" + this.idx);

      var info = jQuery("<div></div>");
      info.addClass("dropzone-info");
      info.attr("id", "dropzone-info-" + this.idx);

      infoBlock.append(info);
      dzone.after(infoBlock);
      dzone.after(infoBar);

    }


  DropZoneUIHandler.prototype.batchStarted = function(){

      if (this.batchId==null) {
        this.buildUI(this.dropZoneId);
        this.batchId = "batch-" + new Date().getTime();
        // Fetch the operation definition
        var o=this; // deRef object !
        console.log("url=" + url);
        var targetUrl = url + "chooseOperation/" + this.batchId;
        var context = jQuery("#" + this.dropZoneId).attr("context");
        targetUrl=targetUrl + "?context=" + context;
        jQuery.get(targetUrl, function(data, textStatus) {
          //console.log(data);
          o.operationsDef=JSON.parse(data);
          console.log(o);
        });
      }
      jQuery("#dropzone-bar-msg-" + this.idx).html("import in progress ...");
      jQuery("#dropzone-bar-btn-" + this.idx).css("visibility","hidden");
      return this.batchId;
  }


  DropZoneUIHandler.prototype.batchFinished = function(batchId) {
    jQuery("#dropzone-bar-msg-" + this.idx).html("upload completed, click to start the import");
    jQuery("#dropzone-bar-btn-" + this.idx).css("visibility","visible");
    jQuery("#dropzone-bar-btn-" + this.idx).attr("value","Continue");
    var o=this; // deRef object !
    jQuery("#dropzone-bar-btn-" + this.idx).bind("click",function(event) {o.selectOperation(batchId, o.dropZoneId, o.url)});
  }

  DropZoneUIHandler.prototype.updateForm  = function (event, value) {
    console.log("updateForm : " + value);
    for (i=0; i< this.operationsDef.length; i++) {
      if(this.operationsDef[i].id==value) {
       var desc = jQuery("<div></div>");
       desc.html(this.operationsDef[i].label + "<br/>");
       jQuery("#dndSubForm").html(desc);
       for (j=0; j< this.operationsDef[i].params.length; j++) {
          var line = jQuery("<div></div>");
          line.text(this.operationsDef[i].params[j].name);
          var input = jQuery("<input></input>");
          input.attr("name", this.operationsDef[i].params[j].name);
          line.append(input);
          jQuery("#dndSubForm").append(line);
        }
        break;
      }
    }
    var panel = jQuery("#dndFormPanel");
    var dZone = jQuery("#" + this.dropZoneId);
    var panelPosition =dZone.position();
    jQuery("body").append(panel);
    if (panel.width() > 0.8*dZone.width()) {
      panel.css("width",0.8*dZone.width());
    }
    panelPosition.top = panelPosition.top + dZone.height()/2 - panel.height()/2;
    panelPosition.left = panelPosition.left + dZone.width()/2 - panel.width()/2;
    panel.css(panelPosition);

    console.log("panel width=" + panel.width());
  }

  DropZoneUIHandler.prototype.selectOperation  = function (batchId, dropId, url) {

    var o=this; // deRef object !
    console.log("url=" + url);
    //var targetUrl = url + "chooseOperation/" + batchId;
    //var context = jQuery("#" + dropId).attr("context");
    //targetUrl=targetUrl + "?context=" + context;
    //jQuery.get(targetUrl, function(data, textStatus) {
    //  jQuery("#" + dropId).html(data);
    //});
    if (this.operationsDef==null) {
      console.log("No OpDEf found !!!");
    } else {
      if (this.operationsDef.length==1 && this.operationsDef[0].params.length==0) {
        // XXX start operation right now
      }
    }

    // Build the form
    console.log("build form");
    var panel = jQuery("<div></div>");
    var form = jQuery("<form name=\"operationForm\"></form>");
    var selector = jQuery("<select name=\"operationId\"></select>");
    selector.attr("id","operationId");
    for (i=0; i< this.operationsDef.length; i++) {
      var optionEntry = jQuery("<option></option>")
      optionEntry.attr("value",this.operationsDef[i].id);
      optionEntry.text(this.operationsDef[i].id); // XXX need a short label
      selector.append(optionEntry);
    }
    selector.bind("change", function(event) {o.updateForm(event, selector.val())});
    form.append(selector);
    var subForm = jQuery("<div></div>");
    subForm.attr("id","dndSubForm");
    form.append(subForm);
    var buttonForm = jQuery("<input type='button'/>");
    buttonForm.attr("value","Import");
    form.append(buttonForm);
    panel.append(form);

    panel.attr("id", "dndFormPanel");
    panel.css("position","absolute");
    panel.css("border","solid 1px black");
    panel.css("background-color","rgba(255,255,255,0.8");
    panel.css("padding","4px");
    panel.css("margin","4px");
    var panelPosition =jQuery("#" + dropId).position();
    jQuery("body").append(panel);
    this.updateForm(null,this.operationsDef[0].id);

    jQuery("#dropzone-bar-msg-" + this.idx).html("Select the import operation");
    //jQuery("#dropzone-bar-btn-" + this.idx).attr("value","Finish");
    jQuery("#dropzone-bar-btn-" + this.idx).css("display","none");

    buttonForm.bind("click", function(event) {
        var formParams = {};
        // XXX
        jQuery("#dndSubForm>div>:input").each(function(index,element) {
           formParams[element.name] = escape(element.value);
        });

        formParams['operationId'] = jQuery("#operationId").val();
        formParams['batchId'] = o.batchId;

        var automationParams = {};
        //automationParams.input = {};
        automationParams.params = formParams;
        automationParams.context = {};

        var targetUrl = o.url + "execute"

        jQuery.ajax({
            type: 'POST',
            contentType : 'application/json+nxrequest',
            data: JSON.stringify(automationParams),
            url: targetUrl,
            timeout: 2000,
            error: function() {
              console.log("Failed to submit");
            },
            success: function(r) {
              console.log("Executed OK");
              //o.removeDropPanel(dropId,batchId);
            }
          })

    });

    jQuery("#dropzone-bar-btn-" + this.idx).bind("click",function(event) {
       var inputs = [];
       // XXX
       jQuery("form[name='operationForm']>:input").each(function(index,element) {
          inputs.push(element.name + '=' + escape(element.value));
       });
       jQuery.ajax({
           type: 'POST',
           data: inputs.join('&'),
           url: jQuery("form[name='operationForm']").attr('action'),
           timeout: 2000,
           error: function() {
             //console.log("Failed to submit");
           },
           success: function(r) {
             o.removeDropPanel(dropId,batchId);
           }
         })
    });

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

(function($) {

   $.fn.nxDropZone = function ( options ) {

     this.each(function(){
       var dropId = jQuery(this).attr("id");
       ids.push(dropId);
       //console.log("Init handler for " + dropId)
       // create UI handler
       var uiHandler = new DropZoneUIHandler(NxDropZoneHandlerIdx++, dropId, options.url);
       // copy optionMap
       var instanceOptions = jQuery.extend({},options);
       // register callback Handler
       instanceOptions.handler = uiHandler;
       jQuery("#" + dropId).dropzone(instanceOptions);
     })

      // bind events on body to show the drop box
      document.body.ondragover = function(event) {highlightDropZones(event)};
      document.body.ondragleave = function(event) {removeHighlights(event)};

     };

     function highlightDropZones(event) {

         if (highLightOn) {
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
                 //console.log("no data");
                 //console.log(dt);
               }
          } else {
              //console.log("No dataTransfer");
          }
      };

      function removeHighlights(event) {
          if (!highLightOn) {
              return;
          }
          //console.log(event);
          jQuery.each(ids, function (idx,id) {
            jQuery("#"+id).removeClass("dropzoneHL");
//          if(!event.relatedTarget || event.relatedTarget.id!=id) {
//            jQuery("#"+id).removeClass("dropzoneHL")
//          }
          });
          highLightOn = false;

      }

 })(jQuery);
