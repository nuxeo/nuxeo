<html>
<body>

<style>
 #backgroundPopup{
 display:none;
 position:fixed;
 height:100%;
 width:100%;
 top:0;
 left:0;
 background:#000000;
 border:1px solid #cecece;
 z-index:1;
 }
 #popupChooser{
 display:none;
 position:fixed;
 height:450px;
 width:780px;
 background:#FFFFFF;
 border:2px solid #cecece;
 z-index:2;
 padding:12px;
 font-size:13px;
 }
 #popupChooser h1{
 text-align:left;
 color:#6FA5FD;
 font-size:22px;
 font-weight:700;
 border-bottom:1px dotted #D3D3D3;
 padding-bottom:2px;
 margin-bottom:20px;
 }
 #popupChooserClose{
 font-size:14px;
 line-height:14px;
 right:6px;
 top:4px;
 position:absolute;
 color:#6fa5fd;
 font-weight:700;
 display:block;
 }
 #button{
 text-align:center;
 margin:100px;
 }
</style>
<script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
<script>
var jqw=$;

function addGadget(name,url) {
 alert(url + ":" + name);
}

</script>
<script type="text/javascript" src="${skinPath}/scripts/gadget-popup.js"></script>


<div id="popupChooser">
         <a id="popupChooserClose">x</a>
         <h1>Nuxeo OpenSosial Gadget selection</h1>
         <p id="popupContent">
         ... Loading ...
         </p>
     </div>
<div id="backgroundPopup"></div>

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