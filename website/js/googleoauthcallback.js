
console.log('entered google oauth callback')

// console.log(window.location);
// console.log(window.location.hash);
// console.log(window.location.href);

// const queryString = window.location.href.split('#')[1];
// console.log(queryString);

// const urlParams = new URLSearchParams(queryString);

// const state = urlParams.get('state');
// const redirect_uri = urlParams.get('redirect_uri');
// const client_id = urlParams.get('client_id');

// console.log(state);
// console.log(redirect_uri);
// console.log(client_id);

function parseURLParams(url) {
    let paramsString = url.split('#')[1];
    console.log(paramsString);

    let params = paramsString.split('&');
    console.log(params);

    let paramsDict = {};

    for (let param in params) {
        let spl = params[param].split('=');
        paramsDict[spl[0]] = spl[1];
    }
    console.log(paramsDict);
    return paramsDict;
}

async function isHandleAvailable(potential) {
    if (potential.length == 0) {
        return false;
    }
    accountInfo = await requestAccountInfo(getAccessToken());
    return accountInfo == null;
}

async function onLoad() {
    params = parseURLParams(window.location.href);

    const accessToken = params['access_token'];
    const expiresIn = params['expires_in'];

    setCookie('signedin', 'googletoken', expiresIn);
    setCookie('googletoken', accessToken, expiresIn);

    console.log('signedin = ' + getCookie('signedin'));
    console.log('token = ' + getCookie('googletoken'));

    populateAccountInfo();
    // TODO show server error here
}

async function populateAccountInfo() {
    // send GET to localhost:7070/account
    console.log('awaiting account info');
    let token = getAccessToken();
    console.log('token = ' + token);
    accountInfo = await requestAccountInfo(token);
    console.log('received account info ' + accountInfo);
    if (accountInfo) {
        console.log('redirecting to home');
        document.location.href="/";
    }
    else {
        return false;
    }
}

async function submitForm() {
    console.log("submitting form")
    let result = await registerAccount(getAccessToken(), id("handle-input").value);
    console.log("result of registerAccount: " + result);

    populateAccountInfo();
}

window.onload = onLoad;

id("handle-input").addEventListener('input', e => {
    let potential = e.target.value;
    if (isHandleAvailable(potential)) {
        console.log(potential + ' is available');
        id("submit").removeAttribute("disabled");
    }
    else {
        console.log(potential + ' is not available');
        id("submit").disabled = "disabled";
    }
});

