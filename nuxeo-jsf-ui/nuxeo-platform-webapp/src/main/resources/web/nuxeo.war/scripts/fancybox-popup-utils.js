function paramIsNumber(value) {
  return ! isNaN (value-0) && value != null;
}
/**
 * Helper function to show content inside a FancyBox popup
 *
 * @deprecated since 5.7. Use openFancyBox(ele, options)
 */
function showFancyBox(ele, width, height, scrolling) {
  width = width || "90%";
  height = height || "90%";
  scrolling = scrolling || "auto";

  if (paramIsNumber(width)) {
    width = parseInt(width);
  }
  if (paramIsNumber(height)) {
    height = parseInt(height);
  }

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
    'centerOnScroll': true,
    'scrolling': scrolling
  }).click();
}

function openFancyBox(ele, options) {
  var name, settings, option,
    popupType = 'iframe';
  if(ele.indexOf("#") == 0) {
    popupType = 'inline';
  }

  // remove all empty options
  for (name in options) {
    if (options.hasOwnProperty(name)) {
      option = options[name];
      if (option === null || option === '') {
        delete options[name]
      }
    }
  }

  settings = {
    'type'     : popupType,
    'autoScale': true,
    'autoDimensions': true,
    'width': '90%',
    'height': '90%',
    'modal': false,
    'transitionIn': 'none',
    'transitionOut': 'none',
    'enableEscapeButton': true,
    'centerOnScroll': true,
    'scrolling': 'auto',
    'padding': 0
  };

  if (options) {
    jQuery.extend(settings, options);
  }

  if (paramIsNumber(settings.width)) {
    settings.width = parseInt(settings.width);
  }
  if (paramIsNumber(settings.height)) {
    settings.height = parseInt(settings.height);
  }

  // ensure fancybox is initialized
  jQuery.fancybox.init();
  jQuery('<a href="' + ele + '"></a>').fancybox(settings).click();
}
