package io.github.bonigarcia.webdriver.jupiter.ch09.network_traffic;

import com.kazurayam.subprocessj.CommandLocator;
import io.appium.mitmproxy.InterceptedMessage;
import io.appium.mitmproxy.MitmproxyJava;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.webdriver.jupiter.ch09.performance.HarCreatorMitmJupiterTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class CaptureNetworkTrafficMitmJupiterTest {

    static Logger log = LoggerFactory.getLogger(HarCreatorMitmJupiterTest.class);

    private WebDriver driver;

    private static final String MITMDUMP_COMMAND_PATH;
    static {
        MITMDUMP_COMMAND_PATH =
                // (1)
                // com.kazurayam.subprocessj.CommandLocator.find("mitmdump") will
                // find the path of mitmdump executable in the current environment.
                // e.g. on my Mac, "/Users/kazurayam/.local/bin/mitmdump"
                CommandLocator.find("mitmdump").command();
    }

    private static final int PROXY_PORT = 8080;
    private MitmproxyJava proxy;

    @BeforeEach
    void setup() throws IOException, TimeoutException {
        log.info("mitmdump command path: " + MITMDUMP_COMMAND_PATH);
        proxy = new MitmproxyJava(MITMDUMP_COMMAND_PATH,    // (3)
                (InterceptedMessage m) -> {                    // (4)
                    // the mitmdump process notify the caller of
                    // the all intercepted messages in event-driven manner
                    System.out.println(String.format("%s", InterceptedMessageFormatter.format(m)));
                    return m;
                });                         // (6)
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
     */
    @Test
    void testCaptureNetworkTraffic() {
        // (11)
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/login-form.html");
        driver.findElement(By.id("username")).sendKeys("user");
        driver.findElement(By.id("password")).sendKeys("user");
        driver.findElement(By.cssSelector("button")).click();
        String bodyText = driver.findElement(By.tagName("body")).getText();
        assertThat(bodyText).contains("Login successful");
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
            proxy.stop();
        }
    }

}
