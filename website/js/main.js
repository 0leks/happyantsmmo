
id("play-coin-game").addEventListener("click", () => document.location.href="/coin");

function refreshPageStatus() {
    requestAllAccounts().then(result => {
        let data = JSON.parse(result);
        console.log(data);

        id("total-accounts").innerHTML = data['total-accounts'];
        id("currently-playing").innerHTML = data['currently-playing'];
        id("account-list").innerHTML = data['account-list'].map(account => {
            return "<li>" + account.handle + (('numcoins' in account) ? " " + account.numcoins + " coins" : "") + "</li>";
        }).join("");
    });
    

    if (getSessionToken() == null) {
        id("play-coin-game").disabled = true;
    }
    else {
        id("play-coin-game").disabled = false;
    }
}


function onSuccess(googleUser) {
    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
    console.log(googleUser);

    let id_token = googleUser.getAuthResponse().id_token;
    console.log(id_token);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', SERVER_PROTOCOL + SERVER_URL + 'googlesignin');
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xhr.onload = function() {
        console.log(xhr.response);
        if (xhr.status == 401) {
            // UNAUTHORIZED, bad id_token
            return;
        }
        else if (xhr.status == 500) {
            // INTERNAL_SERVER_ERROR
            return;
        }
        let result = JSON.parse(xhr.responseText);
        setSessionToken(result.session);
        if ('account' in result) {
            console.log("Account exists");
            signedIn(result.account);
            document.location.href = "/";
        }
        else {
            console.log("Account does not exist");
            document.location.href = "/createaccount";
        }
    };
    xhr.send(id_token);


    // console.log(googleUser.getAuthResponse());
    // let redirecturl = window.location.origin + '/googleoauthcallback#'
    //                         + 'id_token=' + id_token //googleUser.wc.access_token +
    //                         + '&expires_in=' + googleUser.getAuthResponse().expires_in;
    // console.log(redirecturl);
    // document.location.href = redirecturl;

}
function onFailure(error) {
    console.log(error);
}

refreshPageStatus();
