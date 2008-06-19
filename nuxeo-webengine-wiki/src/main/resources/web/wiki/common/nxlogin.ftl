<script>
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
    
/*    $('#login_form').submit(function() {
      
      username = $('#username')[0].value;
      password = $('#password')[0].value;
      
      if (username == null || password == null) {
        showLoginError("Username and Password fields have to be filled in.");
        return false;
      }
      
      loggedin = doLogin(username, password, "/nuxeo/site/login");
      
      if (loggedin) {
        //login success
        document.location.reload();
      } else {
        //login failed
        showLoginError("Username or password incorrect.")
      }
      return false;
    })*/
    
  })
</script>

  <#if (base.user.isAnonymous())>
  
  <div id="logstate">Hi Guest! You are not logged in.</div>
  
  <div id="login">
  <form id="login_form" method="post" action="">
    <input type="text" name="userid" id="username" value="Username" class="username"> <input type="password" name="password" id="password" value="password" class="password">
    <input type="submit" name="nuxeo_login" value="Log In" id="login" class="button">
  </form>
  </div>
<#else>
  <div id="logstate">You are logged in as ${base.user}</div>
  <a href="" id="logout">Logout</a>
  
</#if>