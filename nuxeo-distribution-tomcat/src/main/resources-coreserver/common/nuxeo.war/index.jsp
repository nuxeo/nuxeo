<html>
  <head>
    <title>Nuxeo CMIS</title>
    <link href="/nuxeo/icons/favicon.png" type="image/png" rel="icon">
    <style type="text/css">
<!--
 body {
  font: normal 11px "Lucida Grande", sans-serif;
  background-color:#000;
  color: #343434;
  }

H1 {
  color:#343434;
  font:bold 14px "Lucida Grande", sans-serif;
  padding:0;
  margin:2px 0 15px 0;
  border-bottom:1px dotted #8B8B8B;
  }

H2 {
  color:#999;
  font:bold 10px "Lucida Grande", sans-serif;
  padding:0;
  margin:0 0 0 0;
  }


.login {
  background:#fff;
  opacity:0.8;
  filter : alpha(opacity=80);
  border: 1px solid #4E9AE1;
  padding:20px 75px 5px 70px;
  width:250px;
  }

.login_label {
  font:bold 10px "Lucida Grande", sans-serif;
  text-align: right;
  color: #454545;
  margin:0 4px 0 0;
  width:70px;
  }

.login_input {
  border:1px inset #454545;
  background: white;
  padding:3px;
  color: #454545;
  margin:0 10px 5px 0px;
  font:normal 10px "Lucida Grande", sans-serif;
  }

.formTitle {
  margin:0 0 20px 0;
  text-align:center;
  color:#4a4a4a;
  font-size:14px;
  }

.footer {
  color: #d6d6d6;
  font-size: 9px;
  }

.loginLegal {
  padding: 0;
  margin: 0 0 10px 0;
  }

.version {
  padding-right:50px;
  }

.block_container {
  margin-right:50px;
  border:none;
  height:500px;
  width:350px;
  overflow:auto;
  background-color:#ffffff;
  opacity:0.8;
  filter : alpha(opacity=80);
  }

.errorMessage {
  color:#000;
  font:bold 10px "Lucida Grande", sans-serif;
  border:1px solid #666;
  background: url(/nuxeo/img/warning.gif) 2px 3px no-repeat #FFCC33;
  margin-bottom:12px;
  display:block;
  padding:5px 5px 5px 23px;
  text-align: center;
  }


.welcome {
  background:#fff;
  opacity:0.8;
  filter : alpha(opacity=80);
  border: 1px solid #4E9AE1;
  width:400px;
  padding:20px;
  margin: 150px auto;
  }

.welcomeText {
  font: 12px "Lucida Grande", sans-serif;
  text-align: left;
  color: #454545;
  margin:0 0 0.8em 0;
  }

.footer {
  font: 9px "Lucida Grande", sans-serif;
  text-align: center;
  color: #ededed;

  }

-->

</style>
  </head>
  <body>
  <div class="header"><img src="img/cmis_logo.png"></div>
    <div class="welcome">
               <p class="welcomeText">
                 Welcome to your Nuxeo CMIS server.
               </p>
               <p class="welcomeText">
                 Connect with CMIS using the URL
                 <a href="atom/cmis"><%=request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()%>/nuxeo/atom/cmis</a>
                 for the AtomPub service document.
               </p>
               <p class="welcomeText">
                 You'll find more information about Nuxeo and CMIS on the
                 <a href="https://doc.nuxeo.com/x/JIAO">Nuxeo CMIS Wiki</a>.<br/>
                 You can also discuss your Nuxeo and CMIS experience on the
                 <a href="http://forum.nuxeo.com/f/12/">Nuxeo CMIS forum</a>.
               </p>
    </div>
    <div class="footer">Copyright &copy; 2010 Nuxeo and its respective authors.</div>
  </body>
</html>
