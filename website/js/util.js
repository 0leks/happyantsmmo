
// get elements using id('<elementid>');
let id = id => document.getElementById(id);

function setCookie(name, value, expires_in) {
    document.cookie = name + '=' + value + '; max-age=' + expires_in + '; path=/';
}

function clearCookie(name) {
    document.cookie = name + '=; max-age=0; path=/';
}

function getCookie(c_name) {
    if (document.cookie.length > 0) {
        c_start = document.cookie.indexOf(c_name + "=");
        if (c_start != -1) {
            c_start = c_start + c_name.length + 1
            c_end = document.cookie.indexOf(";", c_start)
            if (c_end == -1) c_end = document.cookie.length
            return document.cookie.substring(c_start, c_end);
        }
    }
    return "";
}

function getAccessToken() {
    let accountInfo = getCookie('signedin');
    if (accountInfo == '') {
        return null;
    }
    return getCookie(accountInfo);
}

function signOut() {
    console.log("signing out");

    let accountInfo = getAccessToken();
    if (accountInfo) {
        clearCookie(accountInfo);
    }
    clearCookie('signedin');
}

function makeRequest(method, endpoint, params, body) {
    return new Promise(function (resolve, reject) {
        let xhr = new XMLHttpRequest();
        let url = 'https://happyantsmmoserver.azurewebsites.net/' + endpoint + (params ? ('?' + params) : '');
        // let url = 'http://localhost/' + endpoint + (params ? ('?' + params) : '');
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

function requestAccountInfo(token) {
    return makeSanitizedRequest("GET", 'account', 'token=' + token)
            .then(result => (result.length==0 ? null : result));
}

function registerAccount(token, handle) {
    console.log('POSTING account create');
    return makeRequest('POST', 'account', 'token=' + token, handle);
}

function requestAllAccounts() {
    return makeSanitizedRequest("GET", 'allaccounts');
}