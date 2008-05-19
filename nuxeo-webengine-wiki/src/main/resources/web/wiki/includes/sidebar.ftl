<#if (base.canWrite)>
<div class="sideblock general">
  <p class="createButton">
   <a href="${Root.urlPath}@@create_entry"><span>Add Page!</span></a>
  </p>
</div>  
</#if>


<div class="sideblock general">
  <script>
  
  function doLogout() {
    jQuery.cookie("JSESSIONID", null, {path: "/"});
    document.location.reload();
  }
  
  function make_base_auth(user, pass) {
    var tok = user + ':' + pass;
    var hash = Base64.encode(tok);
    return "Basic " + hash;
  }
  
  $(document).ready(function(){    
    
    $('#logout').click( function() {
      doLogout()
    } );
    
    $('#login_form').submit(function() {
      
      username = $('#username')[0].value
      password = $('#password')[0].value
      auth = make_base_auth(username, password)
      
      //remove the JSESSIONID cookie to initiate a new session
      jQuery.cookie("JSESSIONID", null, {path: "/"});
      
      // now if I join our inputs using '&' we'll have a query string
      jQuery.ajax({
        url: this.action,
        type: "POST",
        timeout: 2000,
        beforeSend : function(req) {
          req.setRequestHeader("Authorization", auth);
        },
        error: function() {
          alert("Cannot send login informations!")
        },
        complete: function(xhr, textStatus) {
          if (xhr.responseText == "Authenticated") {
            document.location.reload()
          } else {
            logstate = $('#logstate')
            logstate.html("login error")
            logstate.css({color: 'red'})
            //alert("login error")
          }
        },
      })
      
      // by default - we'll always return false so it doesn't redirect the user.
      return false;
    })
    
  })  </script>

  <#if (base.user.isAnonymous())>
  
  <div id="logstate">Hi Guest! You are not logged in.</div>
  
  <form id="login_form" method="post" action="/nuxeo/site/login">
    <label for="user_name">Username</label><input type="text" name="user_name" id="username">
    <label for="user_password">Password</label><input type="password" name="user_password" id="password">
    <input type="submit" name="login" value="Log In" id="login">
  </form>
<#else>
  You are logged in as ${base.user.firstName} ${base.user.firstName}.
  <a href="" id="logout">Logout</a>
  
</#if> 
</div>


<!-- let other templates add more things to the sidebar -->

  <@block name="sidebar"/>

  <@block name="seeother-container" ifBlockDefined="seeother">
  <div class="sideblock contextual">
    <h3>Related documents</h3>
    <div class="sideblock-content">
      <@block name="seeother"/>
    </div>  
  </div>
  <div class="sideblock-content-bottom"></div>
  </@block>

<div class="sideblock general">
    <h3>Last Items</h3>
    <ul>
        <#list Root.document.children?reverse as entry>
            <li><a href="${Root.urlPath}/${entry.name}">${entry.title}</a></li>
            <#if entry_index = 3><#break></#if>
        </#list>
    </ul>
</div>



