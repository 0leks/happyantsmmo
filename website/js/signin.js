
// id("google").addEventListener("click", () => signInGoogle());

// function signInGoogle() {
//     // Google's OAuth 2.0 endpoint for requesting an access token
//     var oauth2Endpoint = 'https://accounts.google.com/o/oauth2/v2/auth';

//     // Create <form> element to submit parameters to OAuth 2.0 endpoint.
//     var form = document.createElement('form');
//     form.setAttribute('method', 'GET'); // Send as a GET request.
//     form.setAttribute('action', oauth2Endpoint);

//     // Parameters to pass to OAuth 2.0 endpoint.
//     var params = {'client_id': '729886368028-j3f6iq0nshp3vog9bet3cu79ms34r00s.apps.googleusercontent.com',
//                     'redirect_uri': window.location.origin + '/googleoauthcallback',
//                     'response_type': 'token',
//                     'scope': 'https://www.googleapis.com/auth/userinfo.email',
//                     'include_granted_scopes': 'true',
//                     // TODO: https://developers.google.com/identity/protocols/oauth2/openid-connect#createxsrftoken + 'state='
//                 };

//     // Add form parameters as hidden input values.
//     for (var p in params) {
//         var input = document.createElement('input');
//         input.setAttribute('type', 'hidden');
//         input.setAttribute('name', p);
//         input.setAttribute('value', params[p]);
//         form.appendChild(input);
//     }

//     // Add form to page and submit it to open the OAuth 2.0 endpoint.
//     document.body.appendChild(form);
//     form.submit();
// }


function onSuccess(googleUser) {
    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
    console.log(googleUser);

    let id_token = googleUser.getAuthResponse().id_token;
    console.log(id_token);

    // var xhr = new XMLHttpRequest();
    // xhr.open('POST', 'https://yourbackend.example.com/tokensignin');
    // xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    // xhr.onload = function() {
    // console.log('Signed in as: ' + xhr.responseText);
    // };
    // xhr.send('idtoken=' + id_token);

    console.log(googleUser.getAuthResponse());
    let redirecturl = window.location.origin + '/googleoauthcallback#' + 
                            'access_token=' + googleUser.wc.access_token +
                            '&expires_in=' + googleUser.getAuthResponse().expires_in;
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

