// Script patching the JSF library to let richfaces parameters go through
// when performing an ajax request in a multipart form, see NXP-14230.
// The idea is to trick the js call into thinking form is not multipart only
// while building the transport, to avoid using a frame transport.
if (window.jsf) {
  var jsfAjaxRequest = jsf.ajax.request;
  var getForm = function getForm(element) {
    if (element) {
      var form = jQuery(element).parents("form");
      if (form.length === 0) {
        form = jQuery(document.forms[0]);
      }
      return form;
    }
    return null;
  };
  jsf.ajax.request = function request(source, event, options) {
    var form = getForm(source);
    var cheating = false;
    if (form && form.attr("enctype") == "multipart/form-data") {
      form.attr("enctype", "");
      cheating = true;
    }
    var res = jsfAjaxRequest(source, event, options);
    if (cheating) {
      form.attr("enctype", "multipart/form-data");
    }
    return res;
  };

  var jsfAjaxResponse = jsf.ajax.response;
  jsf.ajax.response = function response(request, context) {
    // `form` might be undefined in the case of a file upload through RichFacesFileUpload
    // As the `jsf.ajax.response` overridden by RichFaces tries to iterate on `context.form.childNodes`,
    // set it as an empty array to avoid JS Errors
    // see NXP-23220
    if (!context.form) {
      context.form = {
        childNodes: []
      }
    }
    return jsfAjaxResponse(request, context);
  };
}
