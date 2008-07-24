package org.nuxeo.ecm.flex.login
{

  import mx.rpc.http.HTTPService;
  import mx.rpc.events.ResultEvent;
  import mx.rpc.events.FaultEvent;
  import mx.controls.Alert;

  public class NuxeoLogin
  {

    private var _service:HTTPService;
    private var _targetURL:String;
    private var _loginSuccessCB:Function;
    private var _loginFailedCB:Function;
    private var _logoutSuccessCB:Function;
    private var _user:Object;
    private var _cbLoginCheck:Function;
    private var _cbLogout:Function;

    public function NuxeoLogin()
    {
      _service = new HTTPService();
      _service.resultFormat= "e4x";
      //_service.concurrency="single";
      _targetURL="/nuxeo/flexlogin/";
    }

    public function get targetUrl(): String
    {
      return _targetURL;
    }

    public function set targetURL(url:String): void
    {
      _targetURL=url;
    }

    public function login(userName:String, password:String):void
    {
      _service.method="POST";
      //_service.showBusyCursor=true;
      _service.addEventListener(ResultEvent.RESULT, loginResultHandler);
      _service.url = _targetURL;
      var params:Object=new Object();
      params['user_name']=userName;
      params['user_password']=password;
      _service.send(params);
    }

    public function logout(cb:Function):void
    {
      _service.method="GET";
      if (cb==null)
         _cbLogout=cb;
      else
         _cbLogout=null;
      _service.addEventListener(ResultEvent.RESULT, logoutResultHandler);
      _service.url = "/nuxeo/logout";
      _service.send();
    }


    public function isLoggedIn(cb:Function):void
    {
      _service.method="GET";
      _cbLoginCheck=cb;
      _service.addEventListener(ResultEvent.RESULT, loginCheckResultHandler);
      _service.url = _targetURL;
      _service.send();
    }

    private function logoutResultHandler(event:ResultEvent):void
    {
      if (_cbLogout==null)
         _cbLogout();
    }

    private function loginCheckResultHandler(event:ResultEvent):void
    {
      var loginResponse:String=_service.lastResult.status;
      if (loginResponse=="OK")
      {
        _user=new Object();
        _user=_service.lastResult.user
        _cbLoginCheck(true);
      }
      else
        _cbLoginCheck(false);
    }

    private function loginResultHandler(event:ResultEvent):void
    {
      var loginResponse:String=_service.lastResult.status;

      if (loginResponse=="OK")
      {
        _user=new Object();
        _user=_service.lastResult.user
         _loginSuccessCB(event);
      }
      else
        _loginFailedCB(new FaultEvent("Login Failed"));
    }

    public function setLoginSucessCallBack(cb:Function):void
    {
      _loginSuccessCB=cb;
    }

    public function setLoginFailedCallBack(cb:Function):void
    {
      _loginFailedCB=cb;
    }

    public function getConnectedUser():Object
    {
      return _user;
    }
  }
}