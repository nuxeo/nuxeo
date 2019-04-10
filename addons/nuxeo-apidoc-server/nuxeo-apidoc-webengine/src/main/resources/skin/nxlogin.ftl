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
<!--
    <form id="login_form" method="post" action="#">
      <input type="text" name="userid" id="username" value="login" size="15" onfocus="if (this.value=='login') this.value=''" onblur="if (this.value=='') this.value='login'">
      <input type="password" name="password" id="password" value="password" size="15" onfocus="if (this.value=='password') this.value=''" onblur="if (this.value=='') this.value='password'">
      <input type="submit" name="nuxeo_login" value="Ok" id="login" class="button">
    </form>
-->
<#else>
  <div class="logout"><span id="logstate">${Context.principal.name}</span>
  <a href="" id="logout">Logout</a></div>
</#if>
