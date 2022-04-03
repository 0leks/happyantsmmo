
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
}

document.body.prepend(document.createElement("header"));

$(function(){
    $("header").load("/header.html", function() {
        console.log("loaded header");
        id("sign-out").addEventListener("click", () => {
            signOut();
            refreshPageStatus();
        });
        id("sign-in").addEventListener("click", () => document.location.href="/signin");
        refreshPageStatus();
    });
});
