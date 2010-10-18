function openWaiter() {
  Dialog.info($('waiter').innerHTML, {className: "waiter",  width:250, height:100, id: "wait-dlg"})
}

function updateWaiterDialog(message)
{
  $('waiter_message').innerHTML=message;
}

function closeWaiter()
{
  Windows.closeAllModalWindows();
  return true;
}
