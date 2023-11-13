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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <a href="https://appiumpro.com/editions/65-capturing-network-traffic-in-java-with-appium">...</a>
 */
public class HarCreatorMitmJupiterTest {

    static Logger log = LoggerFactory.getLogger(HarCreatorMitmJupiterTest.class);

    static TestOutputOrganizer too;

    private WebDriver driver;

    private static final String MITMDUMP_COMMAND_PATH;
    static {
        MITMDUMP_COMMAND_PATH =
                                                        // (1)
                System.getProperty("user.home") + "/" + ".local/bin/mitmdump";
    }

    private static final int PROXY_PORT = 8080;
    private MitmproxyJava proxy;
    private List<InterceptedMessage> messages;

    private Path harPath;

    @BeforeAll
    static void setupClass() {
        too = TestOutputOrganizerFactory.create(HarCreatorMitmJupiterTest.class);
    }

    @BeforeEach
    void setup() throws IOException, TimeoutException {
        messages = new ArrayList<>();

        // start Mitmproxy process with HAR support
        // : https://www.mitmproxy.org/posts/har-support/
        harPath = too.resolveOutput("dump.har");      // (2)
        List<String> extraMitmproxyParams =
                Arrays.asList("--set",
                        // "--set hardump=filepath"
                        String.format("hardump=%s",
                                // the file path should NOT contain
                                // any whitespace characters
                                harPath.toString()));
        log.info("mitmdump command path: " + MITMDUMP_COMMAND_PATH);
        log.info("extraMitmproxyParams=" + extraMitmproxyParams);

        proxy = new MitmproxyJava(MITMDUMP_COMMAND_PATH,    // (3)
                (InterceptedMessage m) -> {                    // (4)
                    // the mitmdump process notify the caller of
                    // the all intercepted messages in event-driven manner
                    log.info("intercepted request for " + m.getRequest().getUrl());
                    messages.add(m);
                    return m;
                    },
                PROXY_PORT,                                    // (5)
                extraMitmproxyParams);                         // (6)

        // Start the Proxy
        proxy.start();                                         // (7)

        // Start Chrome browser via WebDriverManager
        // The browser need to be Proxy-aware.
        ChromeOptions options = makeChromeOptions();
        driver = WebDriverManager.chromedriver()
                .capabilities(options).create();               // (10)
    }

    ChromeOptions makeChromeOptions() {
        // see https://chromedriver.chromium.org/capabilities
        Proxy seleniumProxy = new Proxy();
        seleniumProxy.setAutodetect(false);
        seleniumProxy.setHttpProxy("127.0.0.1:" + PROXY_PORT);  // URLs with scheme "http:" requires this
        seleniumProxy.setSslProxy("127.0.0.1:" + PROXY_PORT);   // URLs with scheme "https:" requires this
        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);                        // (8)
        options.setAcceptInsecureCerts(true);                   // (9)
        return options;
    }

    /**
     * drive browser to interact with the remote website
     *
     * @throws IOException anything may happen
     */
    @Test
    void testCaptureNetworkTraffic() throws IOException {
        // (11)
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/login-form.html");
        driver.findElement(By.id("username")).sendKeys("user");
        driver.findElement(By.id("password")).sendKeys("user");
        driver.findElement(By.cssSelector("button")).click();
        String bodyText = driver.findElement(By.tagName("body")).getText();
        assertThat(bodyText).contains("Login successful");

        // successful in capturing the network traffic?              (12)
        assertThat(messages).hasAtLeastOneElementOfType(InterceptedMessage.class);

        // consume the captured messages
        Path output = too.resolveOutput("testCaptureNetworkTraffic.txt");
        consumeInterceptedMessages(messages, output);              // (13)
    }

    /**
     * print the stringified InterceptedMessages into the output file
     * @param messages List of InterceptedMessage objects
     * @param output Path to write into
     * @throws FileNotFoundException when the parent file is not there
     */
    void consumeInterceptedMessages(List<InterceptedMessage> messages, Path output)
            throws FileNotFoundException {
        PrintWriter pr = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(output.toFile()),
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
        if (proxy != null) {
            proxy.stop();                               // (14)
        }
        log.info("The HAR was written into " +
                TestOutputOrganizer.toHomeRelativeString(harPath));
    }
}
