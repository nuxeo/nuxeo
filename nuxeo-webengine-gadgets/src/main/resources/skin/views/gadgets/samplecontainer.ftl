<!DOCTYPE html>
<html>
<head>
<title>Sample: Light OpenSocial Javascript container</title>
  <link rel="stylesheet" href="${contextPath}/css/light-container-gadgets.css">

  <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="${contextPath}/opensocial/gadgets/js/rpc.js?c=1"></script>
  <script type="text/javascript" src="${contextPath}/js/?scripts=opensocial/cookies.js|opensocial/util.js|opensocial/gadgets.js|opensocial/cookiebaseduserprefstore.js|opensocial/jquery.opensocial.gadget.js"></script>

  <script type="text/javascript">
    $(document).ready(function() {
      $('.gadgets-gadget-chrome').openSocialGadget({
        baseURL: '${contextPath}' + '/',
        language: 'fr',
        gadgetSpecs: ['http://www.labpixies.com/campaigns/todo/todo.xml',
          'http://localhost:8080/nuxeo/site/gadgets/lastdocuments/lastdocuments.xml']
      });
    })
  </script>
</head>
<body>
    <h2>Some gadgets</h2>
    <div class="gadgets-gadget-chrome"></div>
    <div class="gadgets-gadget-chrome"></div>
</body>
</html>
