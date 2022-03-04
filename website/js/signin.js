

function httpGet(theUrl) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false ); // false for synchronous request
    xmlHttp.send( null );
    return xmlHttp.responseText;
}

id("google").addEventListener("click", () => signInGoogle());

function signIn() {
    console.log("signing in");


}

function signOut() {
    console.log("signing out");
}

function signInGoogle() {
    // Google's OAuth 2.0 endpoint for requesting an access token
    var oauth2Endpoint = 'https://accounts.google.com/o/oauth2/v2/auth';

    // Create <form> element to submit parameters to OAuth 2.0 endpoint.
    var form = document.createElement('form');
    form.setAttribute('method', 'GET'); // Send as a GET request.
    form.setAttribute('action', oauth2Endpoint);

    // Parameters to pass to OAuth 2.0 endpoint.
    var params = {'client_id': '729886368028-j3f6iq0nshp3vog9bet3cu79ms34r00s.apps.googleusercontent.com',
                    'redirect_uri': window.location.origin + '/googleoauthcallback',
                    'response_type': 'token',
                    'scope': 'https://www.googleapis.com/auth/userinfo.email',
                    'include_granted_scopes': 'true',
                    // TODO: https://developers.google.com/identity/protocols/oauth2/openid-connect#createxsrftoken + 'state='
                };

    // Add form parameters as hidden input values.
    for (var p in params) {
        var input = document.createElement('input');
        input.setAttribute('type', 'hidden');
        input.setAttribute('name', p);
        input.setAttribute('value', params[p]);
        form.appendChild(input);
    }

    // Add form to page and submit it to open the OAuth 2.0 endpoint.
    document.body.appendChild(form);
    form.submit();
}


