// helper function to show content inside a FancyBox popup
function showFancyBox(ele, width, height) {
  width = width || "90%";
  height = height || "90%";

  var popupType = 'iframe';
  if(ele.indexOf("#") == 0) {
    popupType = 'inline';
  }

  jQuery('<a href="' + ele + '"></a>').fancybox({
    'autoScale': true,
    'type': popupType,
    'width': width,
    'height': height,
    'transitionIn': 'none',
    'transitionOut': 'none',
    'enableEscapeButton': true,
    'centerOnScroll': true
  }).click();
}
