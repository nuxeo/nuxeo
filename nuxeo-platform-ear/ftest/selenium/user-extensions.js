// ajax4jsf testing helper inspired from
// http://codelevy.com/articles/2007/11/05/selenium-and-ajax-requests
/**
 * Registers with the a4j library to record when an Ajax request
 * finishes.
 *
 * Call this after the most recent page load but before any Ajax requests.
 *
 * Once you've called this for a page, you should call waitForA4jRequest at
 * every opportunity, to make sure the A4jRequestFinished flag is consumed.
 */
Selenium.prototype.doWatchA4jRequests = function() {
  var testWindow = selenium.browserbot.getCurrentWindow();
  // workaround for Selenium IDE 1b2 bug, see
  // http://clearspace.openqa.org/message/46135
  if (testWindow.wrappedJSObject) {
      testWindow = testWindow.wrappedJSObject;
  }
  testWindow.A4J.AJAX.AddListener({
    onafterajax: function() {Selenium.A4jRequestFinished = true}
  });
}

/**
 * If you've set up with watchA4jRequests, this routine will wait until
 * an Ajax request has finished and then return.
 */
Selenium.prototype.doWaitForA4jRequest = function(timeout) {
  return Selenium.decorateFunctionWithTimeout(function() {
    if (Selenium.A4jRequestFinished) {
      Selenium.A4jRequestFinished = false;
      return true;
    }
    return false;
  }, timeout);
}

Selenium.A4jRequestFinished = false;
