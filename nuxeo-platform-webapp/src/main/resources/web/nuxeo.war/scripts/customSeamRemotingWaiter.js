// Overload default Seam Remoting waiter message

Seam.Remoting.loadingMsgDiv = null;
Seam.Remoting.displayLoadingMessage = function()
{
  if (!Seam.Remoting.loadingMsgDiv)
  {
    Seam.Remoting.loadingMsgDiv = document.createElement('div');
    var msgDiv = Seam.Remoting.loadingMsgDiv;
    msgDiv.setAttribute('id', 'loadingMsg');

    msgDiv.style.position = "absolute";
    msgDiv.style.top = "0px";
    msgDiv.style.right = "0px";

    document.body.appendChild(msgDiv);

    var img = document.createElement("img");
    img.src= baseURL + "img/standart_waiter.gif";
    msgDiv.appendChild(img);
  }
  else
  {
    Seam.Remoting.loadingMsgDiv.style.visibility = 'visible';
  }
}

Seam.Remoting.hideLoadingMessage = function()
{
  if (Seam.Remoting.loadingMsgDiv)
    Seam.Remoting.loadingMsgDiv.style.visibility = 'hidden';
}
