
async function submitForm() {
    console.log("submitting form")
    let result = await registerAccount(localStorage.session, id("handle-input").value);
    console.log(result);
    signedIn(JSON.parse(result));
    window.location.href = "/";
}
