<script>

function doLogout() {
  jQuery.cookie("JSESSIONID", null, {path: "/"});
  jQuery.post("${Context.loginPath}", {caller: "login", nuxeo_login : "true"});
}

function doLogin(username, password) {
  //jQuery.cookie("JSESSIONID", null, {path: "/"});
  //req = jQuery.post(document.location.pathname, {nuxeo_login : "true", userid : username, password : password});
  var result = false;
  var req = jQuery.ajax({
    type: "POST",
    async: false,
    url: "${Context.loginPath}",
    data: {caller: "login", nuxeo_login : "true", username : username, password : password},
    success: function(html, status) {
      document.location.reload();
      result = true;
    },
    error: function(html, status) {
      result = html.status != 401;
    }
  });
  return result;
}

function showLoginError(errtext) {
  logstate = $('#logstate');
  if (errtext != null) {
    errmsg = "Login Error: " + errtext;
  } else {
    errmsg = errtext;
  }
  logstate.html(errmsg);
  logstate.css({color: 'red'});
}

$(document).ready(function() {

    $('#logout').click( function() {
      doLogout();
    });

    $('#username').focus( function() {
      if (this.value == 'Username') {
        this.value = "";
      }
    });

    $('#password').focus( function() {
      if (this.value == 'password') {
        this.value = "";
      }
    });

    $('#login_form').submit(function() {

      username = $('#username')[0].value;
      password = $('#password')[0].value;

      if (username == null || password == null) {
        showLoginError("Username and Password fields have to be filled in.");
        return false;
      }

      loggedin = doLogin(username, password);
      if (!loggedin) {
        //login failed
        showLoginError("Username or password incorrect.");
      }
      return false;
    });

  });
</script>

<#if (Context.principal.isAnonymous())>
  <div id="logstate">Hi Guest! You are not logged in.</div>
  <div id="login">
    <form id="login_form" method="post" action="@@login">
      <input type="text" name="username" id="username" value="Username" class="username">
      <input type="password" name="password" id="password" value="password" class="password">
      <input type="submit" name="nuxeo_login" value="Log In" id="login" class="button">
    </form>
  </div>
<#else>
  <div id="logstate">You are logged in as ${Context.principal.name}</div>
  <a href="" id="logout">Logout</a>
</#if>
