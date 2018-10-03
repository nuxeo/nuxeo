function AutomationWrapper(operationId, opts) {
  this.operationId = operationId;
  this.opts = opts;
  this.headers = {};
}

AutomationWrapper.prototype.addParameter = function(name, value){
  this.opts.automationParams.params[name]=value;
  return this;
};

AutomationWrapper.prototype.addParameters = function(params){
  jQuery.extend(this.opts.automationParams.params,params);
  return this;
};

AutomationWrapper.prototype.context = function(name, value){
  this.opts.automationParams.context[name]=value;
  return this;
};

AutomationWrapper.prototype.setContext = function(ctxParams){
  jQuery.extend(this.opts.automationParams.context, ctxParams);
  return this;
};

AutomationWrapper.prototype.setTimeout = function(timeout){
  this.opts.execTimeout=timeout;
  return this;
};

AutomationWrapper.prototype.setHeaders = function(headers){
  jQuery.extend(this.headers, headers);
  return this;
};

AutomationWrapper.prototype.execute = function(successCB, failureCB, voidOp){
  var targetUrl = this.opts.url;
  if (targetUrl.indexOf("/", targetUrl.length - 1)==-1) {
    targetUrl = targetUrl + "/";
  }
  targetUrl =  targetUrl + this.operationId;

  if (!voidOp) {
    voidOp=false;
  }
  var timeout = 5+ (this.opts.execTimeout/1000)|0;
  var documentSchemas = this.opts.documentSchemas;
  var repo = this.opts.repository;
  var self = this;
  jQuery.ajax({
      type: 'POST',
      contentType : 'application/json',
      data: JSON.stringify(this.opts.automationParams),
      beforeSend : function (xhr) {
          xhr.setRequestHeader('X-NXVoidOperation', voidOp);
          xhr.setRequestHeader('Nuxeo-Transaction-Timeout', timeout);
          if (documentSchemas.length>0) {
              xhr.setRequestHeader('X-NXDocumentProperties',documentSchemas);
            }
          if (repo) {
              xhr.setRequestHeader('X-NXRepository', repo);
          }
          for (var key in self.headers) {
            xhr.setRequestHeader(key, self.headers[key]);
          }
      },
      url: targetUrl,
      timeout: this.opts.execTimeout,
      log: this.log,
      error: function(xhr, status, e) {
        if(xhr.status==401) {
          window.location.reload();
        }
        if (failureCB) {
            failureCB(xhr,status,"No Data");
          } else {
            this.log("Failed to execute");
            this.log("Error, Status =" + status);
          }
      },
      success: function(data, status,xhr) {
        this.log("Executed OK");
        if (status=="success") {
          successCB(data,status,xhr);
        } else {
          if (failureCB) {
            failureCB(xhr,status,"No Data");
          } else {
            this.log("Error, Status =" + status);
          }
        }
      }
    })
};

AutomationWrapper.prototype.executeGetBlob = function(successCB, failureCB, blobOp){

    var targetUrl = this.opts.url;
    if (targetUrl.indexOf("/", targetUrl.length - 1)==-1) {
      targetUrl = targetUrl + "/";
    }
    targetUrl =  targetUrl + this.operationId;

    if (!blobOp) {
      voidOp=false;
    }
    var timeout = 5+ (this.opts.execTimeout/1000)|0;
    var documentSchemas = this.opts.documentSchemas;
    var repo = this.opts.repository;
    jQuery.ajax({
        type: 'POST',
        contentType : 'application/json',
        data: JSON.stringify(this.opts.automationParams),
        beforeSend : function (xhr) {
            xhr.setRequestHeader('CTYPE_MULTIPART_MIXED', blobOp);
            xhr.setRequestHeader('Nuxeo-Transaction-Timeout', timeout);
            if (documentSchemas.length>0) {
              xhr.setRequestHeader('X-NXDocumentProperties',documentSchemas);
            }
            if (repo) {
                xhr.setRequestHeader('X-NXRepository', repo);
            }
        },
        url: targetUrl,
        timeout: this.opts.execTimeout,
        log: this.log,
        error: function(xhr, status, e) {
          if (failureCB) {
              failureCB(xhr,status,"No Data");
            } else {
              this.log("Failed to execute");
              this.log("Error, Status =" + status);
            }
        },
        success: function(data, status,xhr) {
          this.log("Executed OK");
          if (status=="success") {
            successCB(data,status,xhr);
          } else {
            if (failureCB) {
              failureCB(xhr,status,"No Data");
            } else {
              this.log("Error, Status =" + status);
            }
          }
        }
      })
 };

AutomationWrapper.prototype.log = function (msg) {
  if (window.console) {
    //console.log(msg);
  }
};

AutomationWrapper.prototype.batchExecute = function(batchId, successCB, failureCB, voidOp){

  if (!voidOp) {
    voidOp=false;
  }

  var targetUrl = this.opts.url;
  if (targetUrl.indexOf("/", targetUrl.length - 1)==-1) {
    targetUrl = targetUrl + "/";
  }
  if (targetUrl.indexOf('/upload/' + batchId + '/execute/' + this.operationId)<0) {
    targetUrl = targetUrl + 'upload/' + batchId + '/execute/' + this.operationId;
  }
  var timeout = 5+ (this.opts.execTimeout/1000)|0;
  var documentSchemas = this.opts.documentSchemas;
  var repo = this.opts.repository;
  jQuery.ajax({
      type: 'POST',
      contentType : 'application/json',
      data: JSON.stringify(this.opts.automationParams),
      beforeSend : function (xhr) {
          xhr.setRequestHeader('X-NXVoidOperation', voidOp);
          xhr.setRequestHeader('Nuxeo-Transaction-Timeout', timeout);
          if (documentSchemas.length>0) {
              xhr.setRequestHeader('X-NXDocumentProperties',documentSchemas);
          }
          if (repo) {
              xhr.setRequestHeader('X-NXRepository', repo);
          }
      },
      url: targetUrl,
      timeout: this.opts.execTimeout,
      log: this.log,
      error: function(xhr, status, e) {
        this.log("Failed to execute");
        if (failureCB) {
          var errorMessage = null;
          if (xhr.response) {
            errorMessage =xhr.response;
            var parsedError = errorMessage;
            try {
              parsedError = JSON.parse(errorMessage);
              errorMessage = parsedError.error
            } catch (err) {
              // NOP
            }
          }
          failureCB(xhr,status,errorMessage);
        } else {
            this.log("Error, Status =" + status);
        }
      },
      success: function(data, status,xhr) {
        this.log("Executed OK : " + status);
        if (status === "success" || status === "nocontent") {
          successCB(data,status,xhr);
        } else {
          if (failureCB) {
            failureCB(xhr,status,"No Data");
          } else {
            this.log("Error, Status =" + status);
          }
        }
      }
    })
  };

(function($) {

   $.fn.automation = function ( operationId , options) {
      var opts = jQuery.extend(true, {}, $.fn.automation.defaults, options);
      return new AutomationWrapper(operationId, opts);
   }

   $.fn.automation.defaults = {
        url : nxContextPath + "/site/automation",
        execTimeout : 30000,
        uploadTimeout : 30000,
        documentSchemas : "dublincore",
        automationParams : {
           params : {},
           context : {}
       }
   }

 })(jQuery);
