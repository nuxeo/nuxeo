// workaround for IE bug, see NXP-6759, NXP-7778
if (typeof Sarissa != 'undefined') {
  jQuery.ajaxSetup({
    xhr: function() {
      if (Sarissa.originalXMLHttpRequest) {
        return new Sarissa.originalXMLHttpRequest();
      } else if (typeof ActiveXObject != 'undefined') {
        return new ActiveXObject("Microsoft.XMLHTTP");
      } else {
        return new XMLHttpRequest();
      }
    }
  });
}
