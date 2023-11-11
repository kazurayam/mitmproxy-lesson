package io.github.bonigarcia.webdriver.jupiter.ch09.performance;

import com.kazurayam.unittest.TestOutputOrganizer;
import io.appium.mitmproxy.InterceptedMessage;
import io.appium.mitmproxy.MitmproxyJava;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.webdriver.jupiter.TestOutputOrganizerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <a href="https://appiumpro.com/editions/65-capturing-network-traffic-in-java-with-appium">...</a>
 */
public class CapturingNetworkTrafficChromeJupiterTest {

    static Logger log = LoggerFactory.getLogger(CapturingNetworkTrafficChromeJupiterTest.class);

    static TestOutputOrganizer too;

    private WebDriver driver;

    private static final String MITMDUMP_COMMAND_PATH;
    static {
        MITMDUMP_COMMAND_PATH =
                // on my Mac
                System.getProperty("user.home") + ".local/bin/mitmdump";
    }

    private static final int PROXY_PORT = 8080;
    private MitmproxyJava mitmDump;
    private List<InterceptedMessage> messages;

    @BeforeAll
    static void setupClass() {
        too = TestOutputOrganizerFactory.create(CapturingNetworkTrafficChromeJupiterTest.class);
    }

    @BeforeEach
    void setup() throws IOException, TimeoutException {
        messages = new ArrayList<>();

        // start Mitmproxy process with HAR support
        // : https://www.mitmproxy.org/posts/har-support/
        List<String> extraMitmproxyParams =
                Arrays.asList("--set",
                        String.format("hardump=%s",
                                // the file path should NOT contain
                                // any whitespace characters
                                too.resolveOutput("dump.har")
                                        .toString()));
        log.info("mitmdump command path: " + MITMDUMP_COMMAND_PATH);
        log.info("extraMitmproxyParams=" + extraMitmproxyParams);

        mitmDump = new MitmproxyJava(MITMDUMP_COMMAND_PATH,
                (InterceptedMessage m) -> {
                    // the mitmdump process notify the caller of the all intercepted
                    // messages in event-driven manner
                    log.info("intercepted request for " + m.getRequest().getUrl());
                    messages.add(m);
                    return m;
                    },
                PROXY_PORT,
                extraMitmproxyParams);  // "--set hardump=filepath"

        // Start the Proxy
        mitmDump.start();

        // Start Chrome browser via WebDriverManager
        // The browser need to be Proxy-aware.
        ChromeOptions options = makeChromeOptions();
        driver = WebDriverManager.chromedriver().capabilities(options).create();
    }

    ChromeOptions makeChromeOptions() {
        // see https://chromedriver.chromium.org/capabilities
        Proxy seleniumProxy = new Proxy();
        seleniumProxy.setAutodetect(false);
        seleniumProxy.setHttpProxy("127.0.0.1:" + PROXY_PORT);  // URLs with scheme "http:" requires this
        seleniumProxy.setSslProxy("127.0.0.1:" + PROXY_PORT);   // URLs with scheme "https:" requires this
        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);
        options.setAcceptInsecureCerts(true);
        return options;
    }

    /**
     * drive browser to interact with the remote website
     *
     * @throws IOException anything may happen
     */
    @Test
    void testCaptureNetworkTraffic() throws IOException {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/login-form.html");
        driver.findElement(By.id("username")).sendKeys("user");
        driver.findElement(By.id("password")).sendKeys("user");
        driver.findElement(By.cssSelector("button")).click();
        String bodyText = driver.findElement(By.tagName("body")).getText();
        assertThat(bodyText).contains("Login successful");

        // successful in capturing the network traffic?
        assertThat(messages).hasAtLeastOneElementOfType(InterceptedMessage.class);

        // print out the captured messages into a file
        File output = too.resolveOutput("testCaptureNetworkTraffic.txt").toFile();
        PrintWriter pr = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),
                                StandardCharsets.UTF_8)));
        for (InterceptedMessage m : messages) {
            pr.println(m.toString());
        }
        pr.flush();
        pr.close();
    }

    /**
     * Stop the browser, stop the proxy
     * @throws InterruptedException any interruption
     */
    @AfterEach
    void tearDown() throws InterruptedException {
        if (driver != null) {
            driver.quit();
        }
        if (mitmDump != null) {
            mitmDump.stop();
        }
    }
}
