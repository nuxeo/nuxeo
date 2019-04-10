/* nuxeo-diff-pictures.js
 * 
 */
var NxDiffPictures;
(function scope_NxDiffPictures() {

  var leftDocId, rightDocId, xpath, resultImgObj, fuzzLabelObj, fuzzObj,
    highlightColorObj, lowlightColorObj, highlightColorDropdownObj,
    contextPath,
    lowlightColorDropdownObj,
    resultImgSizeClass = "large"; // WARNING: Must match the original declaration in nuxeo-diff-pictures.xhtml

  NxDiffPictures = this;

  init = function(inParams) {

    // We are in a iFrame opened by nuxeo-diff-content. We can detect
    // the fancybox was closed by using the onunload Window's event.
    // In this event, we send a quick synchronous ajax request
    var windowUnload = window.onunload;
    window.onunload = function(p1, p2, p3) {

      // tell server to cleanup temp files
      // The call must be synchronous in this context
      jQuery.ajax({
        url: contextPath + "/diffPictures?action=cleanup&leftDocId=" + leftDocId + "&rightDocId=" + rightDocId,
        async: false
      });

      if(typeof windowUnload === "function") {
        previousUnloadFunction(p1, p2, p3);
      }
    }

    contextPath = inParams.contextPath;
    leftDocId = inParams.leftDocId;
    rightDocId = inParams.rightDocId;
    xpath = inParams.xpath;

    var resultImgId = inParams.resultImgId

    // Get the jQuery objects once
    fuzzLabelObj = jQuery("#fuzzLabel");
    fuzzObj = jQuery("#fuzzSlider");

    highlightColorObj = jQuery("#highlightColor");
    highlightColorDropdownObj = jQuery("#highlightColor_dropdown");

    lowlightColorObj = jQuery("#lowlightColor");
    lowlightColorDropdownObj = jQuery("#lowlightColor_dropdown");

    fuzzObj.val(inParams.fuzz);
    updateFuzzLabel();

    highlightColorObj.val(inParams.highlightColor);
    lowlightColorObj.val(inParams.lowlightColor);

    resultImgObj = jQuery(document.getElementById(resultImgId));
    updateResultImage();

    initColorMenus();
  }

  // private
  function buildUrl() {
    var url, lowLight, commandLine;
    url = contextPath + "/diffPictures?leftDocId=" + leftDocId + "&rightDocId=" + rightDocId;
    lowLight = lowlightColorObj.val();
    if(lowLight === "" || lowLight.toLowerCase() === "default") {
      commandLine = "diff-pictures-default";
    } else {
      commandLine = "diff-pictures-default-with-params";
    }

    url += "&commandLine=" + encodeURIComponent(commandLine);
    url += "&fuzz=" + encodeURIComponent(fuzzObj.val() + "%");
    url += "&highlightColor=" + encodeURIComponent(highlightColorObj.val());
    url += "&lowlightColor=" + encodeURIComponent(lowLight);
    // In case we are comparing psd, tif, or anything that is not jpeg, png or gif.
    url += "&altExtension=.jpg";

    return url;
  }

  // Private
  function initColorMenus() {

    highlightColorDropdownObj.dropdown({
      onChange : function(value, text, choice) {
        highlightColorObj.val(text);
      }
    });

    lowlightColorDropdownObj.dropdown({
      onChange : function(value, text, choice) {
        lowlightColorObj.val(text);
      }
    });
  }

  updateResultImage = function() {

    var url = buildUrl();

    resultImgObj.attr("alt", "Comparison result not found");
    resultImgObj.attr("src", url);

  }

  changeResultSize = function(inSelect) {

    resultImgObj.removeClass(resultImgSizeClass);
    resultImgSizeClass = inSelect.value;
    resultImgObj.addClass(resultImgSizeClass);
    inSelect.value = "display size";

  }

  updateFuzzLabel = function() {
    fuzzLabelObj.text("(" + fuzzObj.val() + "%)");
    // Ugly workaround of a Chrome big, where the slider is not displayed once you use it
    fuzzObj.blur();
  }

}());
