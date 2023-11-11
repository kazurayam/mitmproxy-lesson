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
- [io.github.bonigarcia.webdriver.jupiter.ch09.performance.CapturingNetworkTrafficChromeJupiterTest](https://github.com/kazurayam/mitmproxy-lesson/blob/develop/app/src/test/java/io/github/bonigarcia/webdriver/jupiter/ch09/performance/CapturingNetworkTrafficChromeJupiterTest.java)

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

#### Install Python3

At first, I needed [Homebrew](https://brew.sh/) that helps every Mac users to install everything. In the command line, I did this (in fact, many years ago).

```
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

I needed to install Python 3. In the commandline, I did the following (in fact, many years ago).

```
$ brew install python3
```

### Installing mitmproxy with websockets module

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

### Running `mitmdump` in the command line

Now I want to test if the `mitmdump` command is up and running on my machine. Let me check it.

I opened a terminal window, where I started the `mitmdump` command:

```
:~
$ mitmdump --set hardump=tmp/sample-dump.har
[10:51:51.352] HTTP(S) proxy listening at *:8080.
```

Please note that I specified the path of HAR file by the `--set hardump=xxxx` command. 

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

![mitmdump_in_action](https://kazurayam.github.io/images/mitmdump_in_action.png)

Then I quit the process by CTRL+C key.

![mitmdump_quit](https://kazurayam.github.io/images/mitmdump_quit.png)

The process stopped. The message told me that, on exit, the `mitmdump` wrote a HAR file on the local disk. Where is the files? --- Remember, I specified the path in the command argument to start it up.

![sample-dump.har](https://kazurayam.github.io/images/sample-dump.har.png)

I could confirm that the `mitmdump` command is working on my machine.

### Running the sample junit5 test

TODO

### Description of the sample test

TODO
