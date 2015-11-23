/**
 * Helper function to open a popup in a new window
 */
function openPopup(url, options) {
  var popup, checkCompleted, settings, listener;

  settings = {
    'width': '1000',
    'height': '650',
    'onClose': function() {},
    'onMessageReceive': function () {}
  };

  if (options) {
    jQuery.extend(settings, options);
  }

  var left = window.screenX + (window.outerWidth / 2) - (settings.width / 2);
  var top = window.screenY + (window.outerHeight / 2) - (settings.height / 2);

  if (typeof settings.onMessageReceive === "function") {
    listener = function(event) {
      settings.onMessageReceive(event);
    };
    window.addEventListener("message", listener);
  }

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
    if (typeof settings.onClose === "function") {
      settings.onClose();
    }
    window.removeEventListener("message", listener);
  }, 100);
}