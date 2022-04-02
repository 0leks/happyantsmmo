
id("sign-out").addEventListener("click", () => {
    signOut();
    refreshPageStatus();
});
id("sign-in").addEventListener("click", () => document.location.href="/signin");
id("play-coin-game").addEventListener("click", () => document.location.href="/coin");

function refreshPageStatus() {
    console.log(document.cookie);
    
    let accessToken = getAccessToken();
    console.log(accessToken);
    if (accessToken) {
        id("sign-out").classList.remove('hidden');
        id("sign-in").classList.add('hidden');
        requestAccountInfo(accessToken).then(result => {
            console.log(result);
            if (result) {
                id("handle").innerHTML = result;
            }
            else {
                signOut();
                refreshPageStatus();
            }
        });
    }
    else {
        id("sign-out").classList.add('hidden');
        id("sign-in").classList.remove('hidden');
        id("handle").innerHTML = '';
    }
    requestAllAccounts().then(result => {
        let data = JSON.parse(result);
        console.log(data);

        id("total-accounts").innerHTML = data['total-accounts'];
        id("currently-playing").innerHTML = data['currently-playing'];
        id("account-list").innerHTML = data['account-list'].map(account => {
            return "<li>" + account.handle + (('numcoins' in account) ? " " + account.numcoins + " coins" : "") + "</li>";
        }).join("");
    });
    
}

refreshPageStatus();
