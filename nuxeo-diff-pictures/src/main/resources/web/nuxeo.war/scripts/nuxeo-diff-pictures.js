/* nuxeo-diff-pictures.js

 */

var NxDiffPictures;
(function scope_NxDiffPictures() {

	var leftDocId, rightDocId,
		resultImgObj,
		fuzzLabelObj, fuzzObj,
		highlightColorObj, lowlightColorObj,
		highlightColorDropdownObj, lowlightColorDropdownObj,
		resultImgSizeClass = "large"; // WARNING: Must mach the original declaration in nuxeo-diff-pictures.xhtml

	NxDiffPictures = this;

	init = function(inParams) {

		leftDocId = inParams.leftDocId;
		rightDocId = inParams.rightDocId;
		
		var resultImgId = inParams.resultImgId

		// Please, do not ask why I have to do that, I have no idea. But if I don't, we have some messy-missing background in the UI
		jQuery("#fancybox-content").first().css("background-color", "white");

		// The code is called twice: When the fancybox is initialized but not
		// yet displayed, and when it is displayed
		// This leads to problem with the cropping tool, which duplicates
		// the picture and losts itself.
		// Also, a quick reminder about using getElementById() with jQuery: we can't
		// use jQuery(@-"#" + inNxDocId) because nuxeo (well, JSF), sometimes adds
		// colons inside the id, so jQuery is lost.
		//debugger;
		var fancybox = jQuery("#fancybox-content");
		if (fancybox && fancybox.is(":visible")) {

			// Get the jQuery objects once
			fuzzLabelObj = jQuery("#fuzzLabel");
			fuzzObj = jQuery("#fuzzSlider");

			highlightColorObj = jQuery("#highlightColor");
			highlightColorDropdownObj = jQuery("#highlightColor_dropdown");

			lowlightColorObj = jQuery("#lowlightColor");
			lowlightColorDropdownObj = jQuery("#lowlightColor_dropdown");
			
			fuzzObj.val(inParams.fuzz);
			updateFuzzLabel();

			highlightColorObj.val( inParams.highlightColor );
			lowlightColorObj.val( inParams.lowlightColor );

			resultImgObj = jQuery(document.getElementById(resultImgId));
			updateResultImage();

			initColorMenus();
  		}
	}

	// private
	function buildUrl() {
		var url = "/nuxeo/diffPictures?leftDocId=" + leftDocId + "&rightDocId=" + rightDocId;

		url += "&fuzz=" + encodeURIComponent(fuzzObj.val() + "%");
		url += "&highlightColor=" + encodeURIComponent(highlightColorObj.val());
		url += "&lowlightColor=" + encodeURIComponent(lowlightColorObj.val());

		return url;
	}

	// Private
	function initColorMenus() {

		highlightColorDropdownObj.dropdown({
			onChange: function(value, text, choice) {
				highlightColorObj.val(text);
			}
		});
		
		lowlightColorDropdownObj.dropdown({
			onChange: function(value, text, choice) {
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
