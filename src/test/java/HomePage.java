import com.saucelabs.junit.ConcurrentParameterized;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.LinkedList;

public class HomePage extends SauceLabs {

    @ConcurrentParameterized.Parameters
    public static LinkedList browsersStrings() {
        LinkedList browsers = new LinkedList();
        browsers.add(new String[]{"Windows 7", "67", "chrome", null, null});
        browsers.add(new String[]{"Windows 10", "67", "chrome", null, null});
        return browsers;
    }

    public HomePage(String os, String version, String browser, String deviceName, String deviceOrientation) {
        super(os, version, browser, deviceName, deviceOrientation);
        logEnabled(true);
    }

    @Test
    public void clickLink() {
        SauceLabs sauceLabs = new SauceLabs();
        String url = "https://www.amazon.co.uk/";
        //System.setProperty("webdriver.chrome.driver", "C:\\Users\\ChandaranN\\Drivers\\ChromeDriver\\chromedriver.exe");
        //WebDriver driver = new ChromeDriver();
        driver.get(url);
        //driver.navigate().to("https://www.amazon.co.uk/");
        driver.findElement(By.id("sp-cc-accept")).click();
    }
}
