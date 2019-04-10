/* nuxeo-diff-pictures.js
 * 
 */
var NxDiffPictures;
(function scope_NxDiffPictures() {
    
  var gParams;

  var resultImgObj, fuzzLabelObj, fuzzObj,
    highlightColorObj, lowlightColorObj, highlightColorDropdownObj,
    lowlightColorDropdownObj,
    resultImgSizeClass = "large"; // WARNING: Must match the original declaration in nuxeo-diff-pictures.xhtml

  NxDiffPictures = this;

  init = function(inParams) {
      
    var diffToolsContainerObj, resultImgContainerObj;
    
    gParams = inParams;

    var resultImgId = gParams.resultImgId

    // Get the jQuery objects once
    fuzzLabelObj = jQuery("#fuzzLabel");
    fuzzObj = jQuery("#fuzzSlider");

    highlightColorObj = jQuery("#highlightColor");
    highlightColorDropdownObj = jQuery("#highlightColor_dropdown");

    lowlightColorObj = jQuery("#lowlightColor");
    lowlightColorDropdownObj = jQuery("#lowlightColor_dropdown");

    fuzzObj.val(gParams.fuzz);
    updateFuzzLabel();

    highlightColorObj.val(gParams.highlightColor);
    lowlightColorObj.val(gParams.lowlightColor);

    resultImgObj = jQuery(document.getElementById(resultImgId));
    updateResultImage();

    initColorMenus();
  }

  // private
  function buildUrl() {
    var url, lowLight, commandLine;
    url = gParams.contextPath + "/diffPictures?leftDocId=" + gParams.leftDocId + "&rightDocId=" + gParams.rightDocId;
    if(UTILS_stringIsNotBlank(gParams.forcedCommand)) {
        commandLine = gParams.forcedCommand;
    } else {
        lowLight = lowlightColorObj.val();
        if(lowLight === "" || lowLight.toLowerCase() === "default") {
          commandLine = "diff-pictures-default";
        } else {
          commandLine = "diff-pictures-default-with-params";
        }
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

function UTILS_stringIsNotBlank(str) {
    
    return typeof str === "string" && str !== "";
}
