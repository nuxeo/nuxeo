<script>

function doLogout() {
  //jQuery.cookie("JSESSIONID", null, {path: "/"});
  jQuery.post(document.location.pathname, {nuxeo_login : "true"})
}

function doLogin(username, password) {
  //jQuery.cookie("JSESSIONID", null, {path: "/"});
  req = jQuery.post(document.location.pathname, {nuxeo_login : "true", userid : username, password : password});
  if (req.status == 200) {
    return true;
  }
  return false;
}

function showLoginError(errtext) {
  logstate = $('#logstate')
  if (errtext != null) {
    errmsg = "Login Error: " + errtext;
  } else {
    errmsg = errtext
  }
  logstate.html(errmsg)
  logstate.css({color: 'red'})
}
 
 $(document).ready(function(){
    
    $('#logout').click( function() {
      doLogout()
    } );
    
    $('#username').focus( function() {
      if (this.value == 'Username') {
       this.value = "" 
      }
    })
    
    $('#password').focus( function() {
      if (this.value == 'password') {
       this.value = "" 
      }
    })

  })
</script>

  <#if (Context.principal.isAnonymous())>
  
  <div id="logstate">Hi Guest! You are not logged in.</div>
  
  <div id="login">
  <form id="login_form" method="post" action="">
    <input type="text" name="userid" id="username" value="Username" class="username">
    <input type="password" name="password" id="password" value="password" class="password">
    <input type="submit" name="nuxeo_login" value="Log In" id="login" class="button">
  </form>
  </div>
<#else>
  <div id="logstate">You are logged in as ${Context.principal.name}</div>
  <a href="" id="logout">Logout</a>
</#if>
