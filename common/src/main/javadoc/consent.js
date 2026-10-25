// MyCelium analytics consent. Shared by the wiki (source of truth), the home
// page and the API docs: copy this file unchanged when it changes.
//
// Nothing loads from Google until the visitor clicks Accept. The answer is
// kept in a first-party cookie on .mycelium-gg.org, so one choice covers
// every MyCelium site; it is never sent to Google. "Cookie settings" in the
// footer reopens the choice, and withdrawing deletes the Analytics cookies.
// With GA_ID empty the script does nothing at all: no banner, no link.
(function () {
  var GA_ID = "G-3CW2H9GV62";
  var PRIVACY_URL = "https://wiki.mycelium-gg.org/privacy#analytics";
  var CONSENT_COOKIE = "mycelium-consent";
  var ONE_YEAR = 60 * 60 * 24 * 365;

  if (!GA_ID) return;

  var host = location.hostname;
  var shared = /(^|\.)mycelium-gg\.org$/.test(host);
  var domainAttr = shared ? "; domain=.mycelium-gg.org" : "";

  function readChoice() {
    var match = document.cookie.match(new RegExp("(?:^|; )" + CONSENT_COOKIE + "=(granted|denied)"));
    return match ? match[1] : null;
  }

  function saveChoice(value) {
    document.cookie = CONSENT_COOKIE + "=" + value + "; max-age=" + ONE_YEAR + "; path=/" + domainAttr +
      "; SameSite=Lax" + (location.protocol === "https:" ? "; Secure" : "");
  }

  function deleteAnalyticsCookies() {
    var names = document.cookie.split("; ").map(function (c) { return c.split("=")[0]; })
      .filter(function (n) { return n === "_ga" || n.indexOf("_ga_") === 0 || n === "_gid"; });
    var domains = ["", "; domain=" + host, "; domain=." + host];
    if (shared) domains.push("; domain=.mycelium-gg.org");
    names.forEach(function (name) {
      domains.forEach(function (d) {
        document.cookie = name + "=; max-age=0; path=/" + d;
      });
    });
  }

  var loaded = false;
  function loadAnalytics() {
    if (loaded) return;
    loaded = true;
    window.dataLayer = window.dataLayer || [];
    window.gtag = function () { window.dataLayer.push(arguments); };
    // Analytics only: no advertising storage or signals, ever.
    window.gtag("consent", "default", {
      analytics_storage: "granted",
      ad_storage: "denied",
      ad_user_data: "denied",
      ad_personalization: "denied"
    });
    window.gtag("js", new Date());
    window.gtag("config", GA_ID, {
      cookie_domain: "auto",
      allow_google_signals: false,
      allow_ad_personalization_signals: false
    });
    var script = document.createElement("script");
    script.async = true;
    script.src = "https://www.googletagmanager.com/gtag/js?id=" + encodeURIComponent(GA_ID);
    document.head.appendChild(script);
  }

  // ---- banner -------------------------------------------------------------
  var STYLE =
    ".mc-consent{position:fixed;left:1rem;right:1rem;bottom:1rem;z-index:50;max-width:34rem;margin-left:auto;" +
    "background:var(--bg,#fff);color:var(--text,#232323);border:2px solid var(--border,#e0e0e0);border-radius:16px;" +
    "box-shadow:4px 4px 0 rgba(0,0,0,.15);padding:1rem 1.1rem;font:15px/1.5 -apple-system,BlinkMacSystemFont," +
    "'Segoe UI',Roboto,Helvetica,Arial,sans-serif}" +
    ".mc-consent h2{all:unset;display:block;font-weight:800;font-size:1.05rem;margin-bottom:.35rem}" +
    ".mc-consent p{margin:0 0 .8rem}" +
    ".mc-consent a{color:inherit}" +
    ".mc-consent .mc-row{display:flex;gap:.6rem;flex-wrap:wrap}" +
    ".mc-consent button{font:inherit;font-weight:700;cursor:pointer;padding:.5rem 1.2rem;border-radius:999px;" +
    "border:2px solid var(--border,#e0e0e0);background:var(--code-bg,#f4f4f4);color:inherit}" +
    ".mc-consent button:hover{border-color:var(--accent,#d32f2f)}" +
    ".mc-consent button:focus-visible{outline:3px solid var(--accent,#d32f2f);outline-offset:2px}" +
    ".mc-consent-link{background:none;border:0;padding:0;font:inherit;color:inherit;text-decoration:underline;cursor:pointer}";

  function addStyle() {
    if (document.getElementById("mc-consent-style")) return;
    var style = document.createElement("style");
    style.id = "mc-consent-style";
    style.textContent = STYLE;
    document.head.appendChild(style);
  }

  function closeBanner() {
    var open = document.querySelector(".mc-consent");
    if (open) open.remove();
  }

  function showBanner(focus) {
    closeBanner();
    addStyle();
    var box = document.createElement("section");
    box.className = "mc-consent";
    box.setAttribute("aria-label", "Analytics consent");
    box.innerHTML =
      "<h2>Can we count visits?</h2>" +
      "<p>With your OK, Google Analytics counts which pages are read, so we know which docs to improve. " +
      "It sets cookies and sends that data to Google. Nothing is tracked unless you accept; change your " +
      "mind any time under <em>Cookie settings</em>. <a href=\"" + PRIVACY_URL + "\">Privacy policy</a></p>" +
      "<div class=\"mc-row\"><button type=\"button\" data-choice=\"granted\">Accept</button>" +
      "<button type=\"button\" data-choice=\"denied\">Decline</button></div>";
    box.addEventListener("click", function (event) {
      var choice = event.target.getAttribute && event.target.getAttribute("data-choice");
      if (!choice) return;
      var before = readChoice();
      saveChoice(choice);
      closeBanner();
      if (choice === "granted") {
        loadAnalytics();
      } else if (before === "granted") {
        // Withdrawn: drop the cookies and reload so the loaded script is gone too.
        deleteAnalyticsCookies();
        location.reload();
      }
    });
    document.body.appendChild(box);
    if (focus) box.querySelector("button").focus();
  }

  function addSettingsLink() {
    var place = document.querySelector(".footer-bottom, .legal-copy, footer");
    if (!place || document.querySelector(".mc-consent-link")) return;
    var link = document.createElement("button");
    link.type = "button";
    link.className = "mc-consent-link";
    link.textContent = "Cookie settings";
    link.addEventListener("click", function () { showBanner(true); });
    addStyle();
    place.appendChild(document.createTextNode(" · "));
    place.appendChild(link);
  }

  function start() {
    addSettingsLink();
    var choice = readChoice();
    if (choice === "granted") {
      loadAnalytics();
    } else if (choice !== "denied") {
      showBanner(false);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
