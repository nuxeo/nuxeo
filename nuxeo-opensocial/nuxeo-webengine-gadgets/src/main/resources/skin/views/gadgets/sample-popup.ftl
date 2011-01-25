<html>
<head>
  <title>
    Sample Gadget chooser popup
  </title>
</head>
<body>

<!-- include JQuery -->
<script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>

<!-- set the JS Callback when a gadget is added -->
<script>
  function addGadget(name,url) {
   alert("Sample callback : added gadget \n" + url + ":" + name);
  }
</script>
<!-- load the popup JS -->
<script type="text/javascript" src="${skinPath}/scripts/gadget-popup.js"></script>

<!-- Html fragment for Popup -->
<div id="popupChooser" style="display:none">
         <a id="popupChooserClose">x</a>
         <h1>Nuxeo OpenSosial Gadget selection</h1>
         <p id="popupContent">
         ... Loading ...
         </p>
     </div>
<div id="backgroundPopup"></div>

<!-- link for trigger Popup display -->
<A href="javascript:showPopup('http://127.0.0.1:8080/nuxeo/site/gadgets/?mode=popup')"> show </A>

<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>

</body>
</html>