<html>
<head>
<title> Nuxeo Server restarting </title>
<link rel="icon" type="image/png" href="${contextPath}/icons/favicon.png" />
<script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
<style>
html, body {
  border:0 none;
  font-family: Verdana;
  margin:0;
  padding:0;
  color:#333;
}
</style>
</head>
<body>
<br/><br/>
<center>
<div id="loading"><h1>${nuxeoctl.restartServer()}</h1><br/><br/>
<img src="${Context.getServerURL().toString()}/${contextPath}/img/big_loading.gif" /></div>
<br/><br/>
You will be automatically redirected to the login page when Nuxeo server is back online.
</center>
<script type="text/javascript">

// start polling after 10s to be sure the sever is begun the restart
setTimeout(startPolling, 10000);

// wait / polls until login page is available again
function startPolling() {
    var intId = setInterval(function isNuxeoReady() {
        $.get("${Context.getServerURL().toString()}/${contextPath}/login.jsp", function(data, textStatus) {
            window.location.href='${contextPath}/';
        });
    }, 2000);
}
</script>
</body>
</html>