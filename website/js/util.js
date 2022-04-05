
let SERVER_URL = 'happyantsmmoserver.azurewebsites.net/';
let SERVER_PROTOCOL = 'https://';
let SERVER_WEBSOCKET_PROTOCOL = 'wss://';
// let SERVER_URL = 'localhost:7070/';
// let SERVER_PROTOCOL = 'http://';
// let SERVER_WEBSOCKET_PROTOCOL = 'ws://';


// get elements using id('<elementid>');
let id = id => document.getElementById(id);

function getSessionToken() {
    if ('session' in localStorage) {
        return localStorage.session;
    }
    return null;
}

function setSessionToken(session) {
    localStorage.session = session;
}

function signOut() {
    console.log("signing out");
    delete localStorage.session;
    delete localStorage.handle;
}

function signedIn(accountInfo) {
    localStorage.handle = accountInfo.handle;
}

function makeRequest(method, endpoint, params, body) {
    return new Promise(function (resolve, reject) {
        let xhr = new XMLHttpRequest();
        let url = SERVER_PROTOCOL + SERVER_URL + endpoint + (params ? ('?' + params) : '');
        xhr.open(method, url);
        xhr.onload = function () {
            if (this.status >= 200 && this.status < 300) {
                resolve(xhr.response);
            } else {
                reject({
                    status: this.status,
                    statusText: xhr.statusText
                });
            }
        };
        xhr.onerror = function () {
            reject({
                status: this.status,
                statusText: xhr.statusText
            });
        };
        xhr.send(body);
    });
}

function makeSanitizedRequest(method, endpoint, params, body) {
    return makeRequest(method, endpoint, params, body)
            .then(result => result.replace(/[^a-z0-9\}\{\]\[\,\":\-]/gi, ''));
}

function requestAccountInfo(id_token) {
    return makeSanitizedRequest("GET", 'account', 'id_token=' + id_token)
            .then(result => (result.length==0 ? null : result));
}

function registerAccount(sessionToken, handle) {
    console.log('POSTING account create');
    return makeRequest('POST', 'account', 'sessionToken=' + sessionToken, handle);
}

function requestAllAccounts() {
    return makeSanitizedRequest("GET", 'allaccounts');
}