//Handle login/logout from Nuxeo WebEngine using ajax calls
//Requires jQuery and the Cookies plugin for jQuery

function doLogout() {
  //jQuery.cookie("JSESSIONID", null, {path: "/"});
  jQuery.post(document.location.pathname, {nuxeo_login : "true"});
  document.location.reload();
}

function doLogin(username, password) {
  //jQuery.cookie("JSESSIONID", null, {path: "/"});
  req = jQuery.post(document.location.pathname,
        {nuxeo_login : "true", userid : username, password : password});
  if (req.status == 200) {
    document.location.reload();
    return true;
  }
  return false;
}

function make_base_auth(user, pass) {
  var tok = user + ':' + pass;
  var hash = Base64.encode(tok);
  return "Basic " + hash;
}

function doLogin2(username, password, loginUrl) {
  auth = make_base_auth(username, password);

  //remove the JSESSIONID cookie to initiate a new session
  jQuery.cookie("JSESSIONID", null, {path: "/"});

  jQuery.ajax({
    url: loginUrl,
    type: "POST",
    async: false,
    timeout: 2000,
    beforeSend : function(req) {
      req.setRequestHeader("Authorization", auth);
    },
    error: function() {
      alert("Cannot send login informations!");
    },
    complete: function(xhr, textStatus) {
      if (xhr.responseText == "Authenticated") {
        loggedin = true;
      } else {
        loggedin = false;
      }
    }
  });
  return loggedin;
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
