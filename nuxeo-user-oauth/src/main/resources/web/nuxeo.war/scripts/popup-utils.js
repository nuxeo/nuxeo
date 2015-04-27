/**
 * Helper function to open a popup in a new window
 */
function openPopup(url, callback, options) {
  var popup, checkCompleted, settings;

  settings = {
    'width': '1000',
    'height': '650'
  };

  if (options) {
    jQuery.extend(settings, options);
  }

  var left = window.screenX + (window.outerWidth / 2) - (settings.width / 2);
  var top = window.screenY + (window.outerHeight / 2) - (settings.height / 2);

  popup = window.open(url, 'popup',
      'height=' + settings.height +
      ',width=' + settings.width +
      ',top=' + top +
      ',left=' + left);

  checkCompleted = setInterval(function () {
    if (!popup || !popup.closed) {
      return;
    }
    clearInterval(checkCompleted);
    callback();
  }, 100);
}