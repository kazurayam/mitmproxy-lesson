package io.github.bonigarcia.webdriver.jupiter.ch09.network_traffic;

import org.junit.jupiter.api.Test;
import com.kazurayam.subprocessj.CommandLocator;
import com.kazurayam.subprocessj.CommandLocator.CommandLocatingResult;

import static org.assertj.core.api.Assertions.assertThat;

public class FindingMitmdumpPathTest {

    /**
     * Trying to find the path of mitmdump on the machine currently I am working on
     *
     * https://kazurayam.github.io/subprocessj/
     */
    @Test
    public void test_findMitmdumpPath() {
        CommandLocatingResult clr = CommandLocator.find("mitmdump");
        printCLR("test_findMitmdumpPath", clr);
        assertThat(clr.returncode()).isEqualTo(0);
        assertThat(clr.command()).contains("mitmdump");

    }

    private void printCLR(String label, CommandLocatingResult clr) {
        System.out.println("-------- " + label + " --------");
        System.out.println(clr.toString());
    }
}
