<html>
  <head>
    <title> Nuxeo Server restarting </title>
    <link rel="icon" type="image/png" href="${contextPath}/icons/favicon.png" />
    <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
    <style>
      html, body {
        background-color: #ffffff;
        border: 0;
        box-sizing: border-box;
        color: #42444e;
        font-family: Helvetica, Arial;
        font-size: 16px;
        height: 100%;
        margin: 0;
        min-height: 100%;
        padding: 0;
        position: relative;
        text-align: center;
      }
      .container {
        display: table;
        height: 100%;
        margin: 0 auto;
        padding: 0 3em;
        text-align: left;
        width: 70%;
      }
      .column {
        display: table-cell;
        height: 100%;
        padding: 1em;
        vertical-align: middle;
      }
      .column.left {
        width: 40%;
      }
      .column.right {
        width: 50%;
      }
      h1 {
        color: #0066FF;
      }
    </style>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
  </head>
  <body>

    <div class="container" id="loading">
      <div class="column left">
        <img src="${Context.getServerURL().toString()}${contextPath}/img/starting.gif" />
      </div>
      <div class="column right">
        <h1>${nuxeoctl.restartServer()}</h1>
        <p>${Context.getMessage('label.serverRestart.message')}</p>
        <p>${Context.getMessage('label.serverRestart.message2')}</p>
      </div>
    </div>

    <script type="text/javascript">
    <!--
    // be sure Ajax Requests will timeout quickly
    $.ajaxSetup( {
      timeout: 8000
    } );

    // start polling after 15s to be sure the server begun the restart
    var currentUrl = "${Context.getServerURL().toString()}${contextPath}";
    var newUrl = "${nuxeoctl.getServerURL()}";
    if (currentUrl == newUrl || newUrl.match("localhost")) // Polling on currentUrl when nuxeo.url is still default.
      setTimeout(startDirectPolling, 15000);
    else
      setTimeout(startIndirectPolling, 15000);

    // polls until login page is available again
    function startIndirectPolling() {
      var intId = setInterval(function isNuxeoReady() {
        var sc = $("#reloadPage");
        if (sc) sc.remove();
        sc = $("<script></script>");
        sc.attr("id","reloadPage");
        sc.attr("src", newUrl + "/runningstatus?info=reload");
        $("body").append(sc);
      }, 10000);
    }

    function startDirectPolling() {
      var intId = setInterval(function isNuxeoReady() {
          $.get(currentUrl + "/login.jsp", function(data, textStatus) {
              window.location.href = currentUrl;
          });
      }, 10000);
    }

    function reload() {
      window.location.href='${nuxeoctl.getServerURL()}/';
    }
    -->
    </script>

  </body>
</html>
