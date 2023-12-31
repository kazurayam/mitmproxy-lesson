# mitmproxy-lesson

- author: kazurayam
- date: 11 Nov, 2023
- project URL: https://github.com/kazurayam/mitmproxy-lesson

## Problem to solve

I read a book [by Boni Garcia, "Hands-On Selenium WebDriver with Java", O'Reilly](https://www.amazon.com/Hands-Selenium-WebDriver-Java-End/dp/1098110005). In the book, I found the Example 9-4 "Test capturing network traffic through BrowserMob proxy" does not work due to "java.lang.NoSuchFieldError: id_RSASSA_PSS_SHAKE128" problem. See the following issue:

- https://github.com/kazurayam/selenium-webdriver-java/issues/25

I found that the problem was caused by a version collision of the encryption library [bouncycastle.org](https://www.bouncycastle.org/) upon which both [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) and [lightbody/BrowserMob proxy](https://github.com/lightbody/browsermob-proxy) depend.

## Solution

I thought that it would be an idea to create another unit-test example in Java of capturing network traffics between HTTP client (browser) and HTTP server using other proxy software. I looked around and found the [mitmproxy](https://www.mitmproxy.org/) project. The [mitmproxy-java](https://github.com/appium/mitmproxy-java), a bridge between Python's mitmproxy and Java programs, is available. They announced [HAR support](https://www.mitmproxy.org/posts/har-support/) at v10.1, Sep 2023. Why not I learn the mitmproxy product and develop a sample test?

I have spent a couple of days, and could make it. This project contains a working example.

- [build.gradle](https://github.com/kazurayam/mitmproxy-lesson/blob/issue3done/app/build.gradle)
- [io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest](https://github.com/kazurayam/mitmproxy-lesson/blob/develop/app/src/test/java/io/github/bonigarcia/webdriver/jupiter/ch09/performance/CapturingNetworkTrafficChromeJupiterTest.java)

This test creates a HAR file like this:

- [Sample HAR file](https://kazurayam.github.io/mitmproxy-lesson/dump.har)

Let me quote a leading part of the HAR file here:

```
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


## Solution description

### Sequence diagram

The following sequence diagram shows how the sample test and the mitmproxy process interact.

![sequence](https://kazurayam.github.io/mitmproxy-lesson/diagrams/out/sequence.png)

### Installing mitmproxy

For a Java programmer who is not well experienced with Python, installing mitmproxy and make it run on her/his machine is the first hurdle.

Here I will describe what I have done on my MackBook.

#### Install Homebrew for Mac

At first, I needed [Homebrew](https://brew.sh/) that helps every Mac users to install everything. In the command line, I did this (in fact, many years ago).

```
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### Installing mitmproxy with websockets module

The home page of [mitmproxy.org](https://www.mitmproxy.org/) tells us that we can install the mitmproxy quick by typing:

```
$ brew install mitmproxy
```

Unfortunately, this is not enough for us. We need to install the mitmproxy and together with the [websockets](https://pypi.org/project/websockets/) module so that our java code can interact with the mitmproxy process. So we would install the mitmproxy using [pipx](https://pypa.github.io/pipx/). Using pipx, we will have a virtual environment for the mitmproxy command isolated from the other python codes we develop. See https://docs.mitmproxy.org/stable/overview-installation/#installation-from-the-python-package-index-pypi for more information.

On Mac in the command line, I installed pipx: 

```
$ brew install pipx
$ pipx ensurepath
```

then I installed the mitmproxy:

```
$ pipx instal mitmproxy
```

plus, I injected the websockets module into the mitmproxy environment:

```
$ pipx inject mitmproxy websockets
```

By the following command, I could check the path of the `mitmdump` command installed:

```
$ which mitmdump
/Users/kazurayam/.local/bin/mitmdump
```

#### Running `mitmdump` in the command line

Now I want to test if the `mitmdump` command is up and running on my machine. Let me check it.

I opened a terminal window, where I started the `mitmdump` command:

```
:~
$ mkdir tmp
:~
$ mitmdump --set hardump=tmp/sample-dump.har
[10:51:51.352] HTTP(S) proxy listening at *:8080.
```

Please note that I specified the path of HAR file by the `--set hardump=xxxx` command. 

Also note that I created a directory `~/tmp` where the HAR file will be written into.

I opened another terminal window, where I run `curl` command to send a HTTP GET request to a web site through the proxy = the process where the `mitmdump` is running while listening to the localhost:8080 port.

```
:~
$ curl -x http://127.0.0.1:8080 --insecure https://bonigarcia.dev/selenium-webdriver-java/login-form.html
```

In response to this request, the web server responded a HTML file. I could see the source printed in the command line:

```
<!DOCTYPE html>
<html lang="en" class="h-100">

<head>
  <title>Hands-On Selenium WebDriver with Java</title>

  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  ...
  (trimmed)
```

I went back to the other window where I ran the `mitmdump` command. The process of `mitmdump` is still running while displaying the summary of captured network traffics.

![mitmdump_in_action](https://kazurayam.github.io/mitmproxy-lesson/images/mitmdump_in_action.png)

Then I quit the process by CTRL+C key.

![mitmdump_quit](https://kazurayam.github.io/mitmproxy-lesson/images/mitmdump_quit.png)

The process stopped. The message told me that, on exit, the `mitmdump` wrote a HAR file on the local disk. Where is the files? --- Remember, I specified the path in the command argument to start it up.

![sample-dump.har](https://kazurayam.github.io/mitmproxy-lesson/images/sample-dump.har.png)

I could confirm that the `mitmdump` command is working on my machine.

### Running the sample junit5 test

Here I assume that you have Java17 installed.

Let me assume you have downloaded the zip file of this project from the [Releases](https://github.com/kazurayam/mitmproxy-lesson/releases) page. You unzip it in any directory you want, and you get a directory `mitmproxy-lesson`. 

Now you want to run a sample test by Gradle:

```
$ cd ${mitmproxy-lesson}
$ ./gradlew :app:test --tests="*CapturingNetworkTraffic*"
```

In the STDOUT section of JUnit report, I could see the test run passed.

```
> Task :app:compileJava NO-SOURCE
> Task :app:processResources NO-SOURCE
> Task :app:classes UP-TO-DATE
> Task :app:compileTestJava
> Task :app:processTestResources UP-TO-DATE
> Task :app:testClasses
13:02:49.606 [Test worker] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- mitmdump command path: /Users/kazuakiurayama/.local/bin/mitmdump
13:02:49.609 [Test worker] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- extraMitmproxyParams=[--set, hardump=/Users/kazuakiurayama/github/mitmproxy-lesson/app/test-output/io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest/dump.har]
13:02:49.612 [Test worker] INFO io.appium.mitmproxy.MitmproxyJava -- Starting mitmproxy on port 8080
13:02:49.727 [WebSocketSelector-26] INFO org.java_websocket.server.WebSocketServer -- websocket server started successfully
13:02:49.736 [Test worker] DEBUG org.zeroturnaround.exec.ProcessExecutor -- Executing [/Users/kazuakiurayama/.local/bin/mitmdump, --anticache, -p, 8080, -s, /private/var/folders/lh/jkh513dn7f3c0j09z131g1z00000gn/T/mitmproxy-python-plugin9594176225767678777.py, --set, hardump=/Users/kazuakiurayama/github/mitmproxy-lesson/app/test-output/io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest/dump.har].
13:02:49.758 [Test worker] DEBUG org.zeroturnaround.exec.ProcessExecutor -- Started Process[pid=16432, exitValue="not exited"]
13:02:50.260 [WebSocketWorker-18] DEBUG org.java_websocket.server.WebSocketServer -- new connection to websocket server/127.0.0.1:61138
13:02:50.287 [Test worker] INFO io.appium.mitmproxy.MitmproxyJava -- Mitmproxy started on port 8080
13:02:50.310 [Test worker] DEBUG io.github.bonigarcia.wdm.WebDriverManager -- Using WebDriverManager 5.6.0
13:02:50.935 [Test worker] DEBUG io.github.bonigarcia.wdm.cache.ResolutionCache -- Resolution chrome=119 in cache (valid until 14:00:57 11/11/2023 JST)
13:02:50.940 [Test worker] DEBUG io.github.bonigarcia.wdm.cache.ResolutionCache -- Resolution chrome119=119.0.6045.105 in cache (valid until 11:41:55 12/11/2023 JST)
13:02:50.942 [Test worker] INFO io.github.bonigarcia.wdm.WebDriverManager -- Using chromedriver 119.0.6045.105 (resolved driver for Chrome 119)
13:02:50.974 [Test worker] DEBUG io.github.bonigarcia.wdm.WebDriverManager -- Driver chromedriver 119.0.6045.105 found in cache
13:02:50.976 [Test worker] INFO io.github.bonigarcia.wdm.WebDriverManager -- Exporting webdriver.chrome.driver as /Users/kazuakiurayama/.cache/selenium/chromedriver/mac64/119.0.6045.105/chromedriver
13:02:58.322 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://accounts.google.com/ListAccounts?gpsia=1&source=ChromiumBrowser&json=standard
13:02:58.339 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:50.226] Loading script /private/var/folders/lh/jkh513dn7f3c0j09z131g1z00000gn/T/mitmproxy-python-plugin9594176225767678777.py
13:02:58.339 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:50.231] HTTP(S) proxy listening at *:8080.
13:02:58.339 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:50.306][127.0.0.1:61139] client connect
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:50.307][127.0.0.1:61139] client disconnect
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:57.959][127.0.0.1:61162] client connect
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:57.999][127.0.0.1:61162] server connect accounts.google.com:443 (142.250.196.109:443)
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.085][127.0.0.1:61165] client connect
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.101][127.0.0.1:61166] client connect
13:02:58.340 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.192][127.0.0.1:61166] server connect bonigarcia.dev:443 (185.199.110.153:443)
13:02:58.341 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.192][127.0.0.1:61165] server connect bonigarcia.dev:443 (185.199.110.153:443)
13:02:58.344 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61162: POST https://accounts.google.com/ListAccounts?gpsia=1&source… HTTP/2.0
13:02:58.345 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 43b
13:02:58.371 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://bonigarcia.dev/selenium-webdriver-java/login-form.html
13:02:58.372 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61166: GET https://bonigarcia.dev/selenium-webdriver-java/login-fo… HTTP/2.0
13:02:58.373 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 1020b
13:02:58.471 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://bonigarcia.dev/selenium-webdriver-java/img/hands-on-icon.png
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.419][127.0.0.1:61169] client connect
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.421][127.0.0.1:61170] client connect
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.423][127.0.0.1:61171] client connect
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.466][127.0.0.1:61169] server connect cdn.jsdelivr.net:443 (151.101.109.229:443)
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.466][127.0.0.1:61171] server connect cdn.jsdelivr.net:443 (151.101.109.229:443)
13:02:58.473 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61166: GET https://bonigarcia.dev/selenium-webdriver-java/img/hand… HTTP/2.0
13:02:58.474 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 8.2k
13:02:58.596 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/css/bootstrap.min.css
13:02:58.601 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.483][127.0.0.1:61170] server connect code.jquery.com:443 (151.101.66.137:443)
13:02:58.601 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.515][127.0.0.1:61171] Client TLS handshake failed. The client disconnected during the handshake. If this happens consistently for cdn.jsdelivr.net, this may indicate that the client does not trust the proxy's certificate.
13:02:58.601 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.517][127.0.0.1:61171] client disconnect
13:02:58.601 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:02:58.518][127.0.0.1:61171] server disconnect cdn.jsdelivr.net:443 (151.101.109.229:443)
13:02:58.602 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61169: GET https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/css/b… HTTP/2.0
13:02:58.602 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 25.7k
13:02:58.613 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://code.jquery.com/jquery-3.6.0.min.js
13:02:58.618 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61170: GET https://code.jquery.com/jquery-3.6.0.min.js HTTP/2.0
13:02:58.618 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 30.2k
13:02:58.625 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/js/bootstrap.bundle.min.js
13:02:58.627 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61169: GET https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/js/bo… HTTP/2.0
13:02:58.627 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 23.4k
13:03:00.104 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://bonigarcia.dev/selenium-webdriver-java/img/hands-on-icon.png
13:03:00.106 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:03:00.027][127.0.0.1:61179] client connect
13:03:00.106 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- [13:03:00.077][127.0.0.1:61179] server connect content-autofill.googleapis.com:443 (142.251.42.202:443)
13:03:00.106 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61166: GET https://bonigarcia.dev/selenium-webdriver-java/img/hand… HTTP/2.0
13:03:00.106 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 8.2k
13:03:00.230 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://content-autofill.googleapis.com/v1/pages/ChVDaHJvbWUvMTE5LjAuNjA0NS4xMjMSIAnLmHAugdKVvhIFDeeNQA4SBQ3OQUx6IQZp6q_8NyIV?alt=proto
13:03:00.232 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61179: GET https://content-autofill.googleapis.com/v1/pages/ChVDaH… HTTP/2.0
13:03:00.233 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 44b
13:03:00.883 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://bonigarcia.dev/selenium-webdriver-java/login-sucess.html?username=user&password=user
13:03:00.884 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61166: GET https://bonigarcia.dev/selenium-webdriver-java/login-su… HTTP/2.0
13:03:00.884 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 701b
13:03:00.951 [WebSocketWorker-18] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- intercepted request for https://bonigarcia.dev/selenium-webdriver-java/img/hands-on-icon.png
13:03:00.952 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava -- 127.0.0.1:61166: GET https://bonigarcia.dev/selenium-webdriver-java/img/hand… HTTP/2.0
13:03:00.952 [Thread-13] INFO io.appium.mitmproxy.MitmproxyJava --      << HTTP/1.1 200 OK 8.2k
13:03:01.495 [WaitForProcess-Process[pid=16432, exitValue="not exited"]] DEBUG org.zeroturnaround.exec.WaitForProcess -- Stopping Process[pid=16432, exitValue="not exited"]...
13:03:01.498 [WebSocketSelector-26] DEBUG org.java_websocket.server.WebSocketServer -- closed /127.0.0.1:61138 with exit code 1001 additional info: 
13:03:01.608 [Test worker] INFO io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest -- The HAR was written into ~/github/mitmproxy-lesson/app/test-output/io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest/dump.har
> Task :app:test
BUILD SUCCESSFUL in 15s
3 actionable tasks: 2 executed, 1 up-to-date
13:03:02: Execution finished ':app:test --tests "io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest"'.

```

The message tells that the HAR file was successfully written into `~/github/mitmproxy-lesson/app/test-output/io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest/dump.har`.

## Description of the sample test

Please refer to [this doc](https://kazurayam.github.io/mitmproxy-lesson/).
