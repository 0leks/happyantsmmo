let showSignInButton = true;
let showSignOutButton = true;
let showAccountInfo = true;

function switchToSignedIn(handle) {
    if (showSignInButton) {
        id("sign-out").classList.remove('hidden');
    }
    if (showSignOutButton) {
        id("sign-in").classList.add('hidden');
    }
    if (showAccountInfo) {
        id("handle").innerHTML = handle;
    }
}

function switchToSignedOut() {
    if (showSignInButton) {
        id("sign-out").classList.add('hidden');
    }
    if (showSignOutButton) {
        id("sign-in").classList.remove('hidden');
    }
    if (showAccountInfo) {
        id("handle").innerHTML = '';
    }
}

function switchToShowNeither() {
    id("sign-out").classList.add('hidden');
    id("sign-in").classList.add('hidden');
    id("handle").innerHTML = '';
}

function refreshPageStatus() {

    console.log(localStorage);

    if (getSessionToken() == null) {
        switchToSignedOut();
    }
    else {
        if ('handle' in localStorage) {
            switchToSignedIn(localStorage.handle);
        }
        else {
            // session exists but account not created
            // make it seem like not logged in
            switchToSignedIn("ACCOUNT NOT CREATED");
        }
    }
}

document.body.prepend(document.createElement("header"));

$(function(){
    $("header").load("/header.html", function() {
        console.log("loaded header");
        id("sign-out").addEventListener("click", () => {
            signOut();
            switchToSignedOut();
            location.reload();
        });
        id("sign-in").addEventListener("click", () => document.location.href="/signin");
        refreshPageStatus();
        if (document.location.href.endsWith('/signin/')) {
            switchToShowNeither();
        }
    });
});
