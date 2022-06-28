<header>
    <div class="title">Happy Ants MMO</div>
    <div id="handle"></div>
    <button id="sign-in" class="">Sign In</button>
    <button id="sign-out" class="hidden">Sign Out</button>

    <a href="#" onclick="signOut();">Sign out</a>
    <script>
    function signOut() {
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
        console.log('User signed out.');
        });
    }
    </script>
</header>