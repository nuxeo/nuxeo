function AutomationWrapper(operationId,opts) {

  this.operationId=operationId;
  this.opts=opts;

  AutomationWrapper.prototype.addParameter = function(name, value){
    this.opts.automationParams.params[name]=value;
    return this;
  }

  AutomationWrapper.prototype.addParameters = function(params){
    jQuery.extend(this.opts.automationParams.params,params);
      return this;
    }

  AutomationWrapper.prototype.context = function(name, value){
    this.opts.automationParams.context[name]=value;
    return this;
  }

  AutomationWrapper.prototype.setContext = function(ctxParams){
    jQuery.extend(this.opts.automationParams.context, ctxParams);
    return this;
  }

  AutomationWrapper.prototype.execute = function(successCB, failureCB, voidOp){
    var targetUrl = this.opts.url + '/' + this.operationId;
    if (!voidOp) {
      voidOp=false;
    }
    jQuery.ajax({
        type: 'POST',
        contentType : 'application/json+nxrequest',
        data: JSON.stringify(this.opts.automationParams),
        beforeSend : function (xhr) {
            xhr.setRequestHeader('X-NXVoidOperation', voidOp);
        },
        url: targetUrl,
        timeout: 30000,
        error: function(xhr, status, e) {
          if (failureCB) {
              failureCB(xhr,status,"No Data");
            } else {
              log("Failed to execute");
              log("Error, Status =" + status);
            }
        },
        success: function(data, status,xhr) {
          log("Executed OK");
          if (status=="success") {
            successCB(data,status,xhr);
          } else {
            if (failureCB) {
              failureCB(xhr,status,"No Data");
            } else {
              log("Error, Status =" + status);
            }
          }
        }
      })
  }
  
  AutomationWrapper.prototype.executeGetBlob = function(successCB, failureCB, blobOp){
	    var targetUrl = this.opts.url + '/' + this.operationId;
	    if (!blobOp) {
	      voidOp=false;
	    }
	    jQuery.ajax({
	        type: 'POST',
	        contentType : 'application/json+nxrequest',
	        data: JSON.stringify(this.opts.automationParams),
	        beforeSend : function (xhr) {
	            xhr.setRequestHeader('CTYPE_MULTIPART_MIXED', blobOp);
	        },
	        url: targetUrl,
	        timeout: 30000,
	        error: function(xhr, status, e) {
	          if (failureCB) {
	              failureCB(xhr,status,"No Data");
	            } else {
	              log("Failed to execute");
	              log("Error, Status =" + status);
	            }
	        },
	        success: function(data, status,xhr) {
	          log("Executed OK");
	          if (status=="success") {
	            successCB(data,status,xhr);
	          } else {
	            if (failureCB) {
	              failureCB(xhr,status,"No Data");
	            } else {
	              log("Error, Status =" + status);
	            }
	          }
	        }
	      })
   }

  AutomationWrapper.prototype.log = function (msg) {
    if (window.console) {
        //console.log(msg);
      }
  }

  AutomationWrapper.prototype.batchExecute = function(batchId, successCB, failureCB, voidOp){

    if (!voidOp) {
      voidOp=false;
    }
    this.addParameter("operationId", this.operationId);
    this.addParameter("batchId", batchId);

    var targetUrl = this.opts.url + '/batch/execute';
    jQuery.ajax({
        type: 'POST',
        contentType : 'application/json+nxrequest',
        data: JSON.stringify(this.opts.automationParams),
        beforeSend : function (xhr) {
            xhr.setRequestHeader('X-NXVoidOperation', voidOp);
        },
        url: targetUrl,
        timeout: 30000,
        error: function(xhr, status, e) {
          log("Failed to execute");
          if (failureCB) {
            var errorMessage = null;
            if (xhr.response) {
              errorMessage =xhr.response;
              var parsedError = JSON.parse(errorMessage);
              if (parsedError && parsedError.error) {
                errorMessage = parsedError.error
              }
            }
            failureCB(xhr,status,errorMessage);
          } else {
              log("Error, Status =" + status);
          }
        },
        success: function(data, status,xhr) {
          log("Executed OK : " + status);
          if (status=="success") {
            successCB(data,status,xhr);
          } else {
            console.log
              if (failureCB) {
                  failureCB(xhr,status,"No Data");
                } else {
                  log("Error, Status =" + status);
                }
          }
        }
      })
    }


}

(function($) {

   $.fn.automation = function ( operationId ) {
      var opts = new Object($.fn.automation.defaults);
      return new AutomationWrapper(operationId, opts);
   }

   $.fn.automation.defaults = {
        url : nxContextPath + "/site/automation",
        automationParams : {
           params : {},
           context : {}
       }
   }

 })(jQuery);
