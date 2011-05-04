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

  AutomationWrapper.prototype.execute = function(successCB, failureCB){
    var targetUrl = this.opts.url + '/' + this.operationId;
    jQuery.ajax({
        type: 'POST',
        contentType : 'application/json+nxrequest',
        data: JSON.stringify(this.opts.automationParams),
        url: targetUrl,
        timeout: 10000,
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

  AutomationWrapper.prototype.batchExecute = function(batchId, successCB, failureCB){

    this.addParameter("operationId", this.operationId);
    this.addParameter("batchId", batchId);

    var targetUrl = this.opts.url + '/batch/execute';
    jQuery.ajax({
        type: 'POST',
        contentType : 'application/json+nxrequest',
        data: JSON.stringify(this.opts.automationParams),
        url: targetUrl,
        timeout: 10000,
        error: function(xhr, status, e) {
          log("Failed to execute");
          if (failureCB) {
              failureCB(xhr,status,"No Data");
            } else {
              log("Error, Status =" + status);
            }
        },
        success: function(data, status,xhr) {
          log("Executed OK : " + status);
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


}

(function($) {

   $.fn.automation = function ( operationId ) {
      var opts = new Object($.fn.automation.defaults);
      return new AutomationWrapper(operationId, opts);
   }

   $.fn.automation.defaults = {
        url : "/nuxeo/site/automation",
        automationParams : {
           params : {},
           context : {}
       }
   }

 })(jQuery);
