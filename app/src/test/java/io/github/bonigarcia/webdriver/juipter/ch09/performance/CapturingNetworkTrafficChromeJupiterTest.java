package io.github.bonigarcia.webdriver.juipter.ch09.performance;

import com.kazurayam.unittest.TestOutputOrganizer;
import io.appium.mitmproxy.InterceptedMessage;
import io.appium.mitmproxy.MitmproxyJava;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.webdriver.juipter.TestOutputOrganizerFactory;
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

    private static final String MITMDUMP_COMMAND_PATH_MAC = "/Users/kazuakiurayama/.local/bin/mitmdump";

    private MitmproxyJava mitmDump;
    private List<InterceptedMessage> messages;

    @BeforeAll
    static void setupClass() {
        too = TestOutputOrganizerFactory.create(CapturingNetworkTrafficChromeJupiterTest.class);
    }

    @BeforeEach
    void setup() throws IOException, TimeoutException {
        messages = new ArrayList<>();

        // start Mitmproxy process
        String mitmdumpCommand = MITMDUMP_COMMAND_PATH_MAC;
        log.info("mitmdump command path: " + mitmdumpCommand);
        mitmDump = new MitmproxyJava(mitmdumpCommand, (InterceptedMessage m) -> {
            System.out.println("intercepted request for " + m.getRequest().getUrl());
            messages.add(m);
            return m;
        });
        mitmDump.start();

        // start Chrome browser
        // see https://chromedriver.chromium.org/capabilities
        Proxy seleniumProxy = new Proxy();
        seleniumProxy.setAutodetect(false);
        seleniumProxy.setHttpProxy("127.0.0.1:8080");
        seleniumProxy.setSslProxy("127.0.0.1:8080");
        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);
        options.setAcceptInsecureCerts(true);
        driver = WebDriverManager.chromedriver().capabilities(options).create();
    }

    @Test
    void testCaptureNetworkTraffic() throws IOException {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/login-form.html");
        driver.findElement(By.id("username")).sendKeys("user");
        driver.findElement(By.id("password")).sendKeys("user");
        driver.findElement(By.cssSelector("button")).click();
        String bodyText = driver.findElement(By.tagName("body")).getText();
        assertThat(bodyText).contains("Login successful");

        // successful capturing?
        assertThat(messages).hasAtLeastOneElementOfType(InterceptedMessage.class);

        // print out the captured messages into a file
        File output = too.resolveOutput("testCaptureNetworkTraffic.json").toFile();
        PrintWriter pr = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),
                                StandardCharsets.UTF_8)));
        for (InterceptedMessage m : messages) {
            pr.println(m.toString());
        }
        pr.flush();
        pr.close();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (mitmDump != null) {
            mitmDump.stop();
        }
        if (driver != null) {
            driver.quit();
        }
    }
}
