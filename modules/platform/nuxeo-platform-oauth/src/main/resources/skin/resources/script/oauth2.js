function openerPostMessage() {
    var token = document.querySelector("[data-token]").dataset.token;
    var data = {
        'token': token
    };
    window.opener.postMessage(JSON.stringify(data), "*");
    window.close();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", openerPostMessage);
} else {
    openerPostMessage();
}
