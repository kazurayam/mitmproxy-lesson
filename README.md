# mitmproxy-lesson

- author: kazurayam
- date: 11 Nov, 2023
- project URL: https://github.com/kazurayam/mitmproxy-lesson

## Problem to solve

I read a book [by Boni Garcia, "Hands-On Selenium WebDriver with Java", O'Reilly](https://www.amazon.com/Hands-Selenium-WebDriver-Java-End/dp/1098110005). In the book, I found the Example 9-4 "Test capturing network traffic through BrowserMob proxy" does not work due to "java.lang.NoSuchFieldError: id_RSASSA_PSS_SHAKE128" problem. See the following issue:

- https://github.com/kazurayam/selenium-webdriver-java/issues/25

I found that the problem was caused by a version collision of the encryption library [bouncycastle.org](https://www.bouncycastle.org/) upon which both [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) and [lightbody/BrowserMob proxy](https://github.com/lightbody/browsermob-proxy) depend.

I thought that it would be an idea to create another unit-test example in Java of capturing network traffics using other HTTPS proxy software. I looked around and found the [mitmproxy](https://www.mitmproxy.org/) project. They announced [HAR support](https://www.mitmproxy.org/posts/har-support/) at v10.1, Sep 2023. The [mitmproxy-java](https://github.com/appium/mitmproxy-java), between Python's mitmproxy and Java programs, is available. Why not I learn the mitmproxy product and develop a sample test?

## Solution

This project contains a working example.

- [build.gradle](https://github.com/kazurayam/mitmproxy-lesson/blob/issue3done/app/build.gradle)
- [io.github.bonigarcia.webdriver.jupiter.ch09.performance.CapturingNetworkTrafficChromeJupiterTest](https://github.com/kazurayam/mitmproxy-lesson/blob/develop/app/src/test/java/io/github/bonigarcia/webdriver/jupiter/ch09/performance/CapturingNetworkTrafficChromeJupiterTest.java)

This test creates a HAR file like this:

- [Sample HAR file](https://kazurayam.github.io/mitmproxy-lesson/dump.har)

Let me quote a leading part here:

[source,json]
```agsl
{
    "log": {
        "version": "1.2",
        "creator": {
            "name": "mitmproxy",
            "version": "10.1.3",
            "comment": ""
        },
        "pages": [],
        "entries": [
            {
                "startedDateTime": "2023-11-10T13:06:47.567873+00:00",
                 "time": 371.8879222869873,
                 "request": {
                     "method": "POST",
                     "url": "https://accounts.google.com/ListAccounts?gpsia=1&source=ChromiumBrowser&json=standard",
                     "httpVersion": "HTTP/2.0",
                     "cookies": [],
                     "headers": [
                         {
...
```


https://docs.mitmproxy.org/stable/overview-getting-started/

## install mitmproxy

https://docs.mitmproxy.org/stable/overview-installation/

On Mac
```
$ pipx instal mitmproxy
```

```
$ pipx inject mitmproxy websockets
```

https://pod.hatenablog.com/entry/2021/06/23/221537

