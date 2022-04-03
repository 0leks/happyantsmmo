

function onSuccess(googleUser) {
    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
    console.log(googleUser);

    let id_token = googleUser.getAuthResponse().id_token;
    console.log(id_token);

    // var xhr = new XMLHttpRequest();
    // xhr.open('POST', SERVER_PROTOCOL + SERVER_URL + 'tokensignin');
    // xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    // xhr.onload = function() {
    //     console.log('Signed in as: ' + xhr.responseText);
    // };
    // xhr.send(id_token);


    console.log(googleUser.getAuthResponse());
    let redirecturl = window.location.origin + '/googleoauthcallback#'
                            + 'id_token=' + id_token //googleUser.wc.access_token +
                            + '&expires_in=' + googleUser.getAuthResponse().expires_in;
    console.log(redirecturl);
    document.location.href = redirecturl;

}
function onFailure(error) {
    console.log(error);
}
function renderGoogleButton() {
    gapi.signin2.render('my-signin2', {
        'scope': 'profile email',
        'width': 200,
        'height': 45,
        'longtitle': true,
        'theme': 'light',
        'onsuccess': onSuccess,
        'onfailure': onFailure
    });
}

