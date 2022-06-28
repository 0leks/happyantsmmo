<!DOCTYPE html>
<html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Happy Ants MMO</title>
        <link rel="stylesheet" href="/css/shared.css">
        <link rel="stylesheet" href="/css/main.css">
        <script
            src="https://code.jquery.com/jquery-3.3.1.js"
            integrity="sha256-2Kok7MbOyxpgUVvAk/HJ2jigOSYS2auK4Pfzbm7uH60="
            crossorigin="anonymous">
        </script>
        <script src="https://apis.google.com/js/platform.js" async defer></script>
    </head>
    <body>
        <!-- <header>
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
        </header> -->
        <?php
            include("header.php");
        ?>
        <section id="content">
            <div id="info">
                <h1>Server Info</h1>
                <div id="total-accounts"> <!-- Built by JS --> </div>
                <div id="currently-playing"> <!-- Built by JS --> </div>
                <div>
                    Account List:
                    <ul id="account-list"> <!-- Built by JS --> </ul>
                </div>
            </div>
            <div>
                <button id="play-coin-game">Play</button>
            </div>
        </section>
        
        <script src="/js/util.js" type="text/javascript"></script>
        <script src="/js/main.js" type="text/javascript"></script>
        <script src="/js/header.js" type="text/javascript"></script>
    </body>
</html>
