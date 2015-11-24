<html>
  <head>
  <script>
    function refreshParent() {
      window.parent.location.href = window.parent.location.href;
    }
  </script>
  </head>
  <body onload="refreshParent()">
    Session timed out !
  </body>
</html>