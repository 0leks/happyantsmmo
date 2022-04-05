
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
    
}

refreshPageStatus();
