
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.junit.ConcurrentParameterized;
import com.saucelabs.junit.SauceOnDemandTestWatcher;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.LinkedList;


/**

 * Includes the {@link SauceOnDemandTestWatcher} which will invoke the Sauce REST API to mark
 * the test as passed or failed.
 *
 * @author Namita Kajani Chandarana
 */
@Ignore
@RunWith(ConcurrentParameterized.class)
public class SauceLabs implements SauceOnDemandSessionIdProvider {

    public static String seleniumURI;
    public static String buildTag;
    //this is a flag which is used to carry out / skip proxy setting
    public static String external = System.getenv("BP_EXTERNAL");
    //setting the sauce labs user credentials which get pulled in via Jenkins variables / Windows user variables / lastly a specific .on-demand file in the Windows user home directory
    protected static String username = System.getenv("SAUCE_USERNAME");
    protected static String accesskey = System.getenv("SAUCE_ACCESS_KEY");
    //proxy settings to testing services account - handles proxy connection but possibly can be removed once proxy tunnel is created with sauce (takes in credentials from env variables)
    final String authUser = "-svc-ist-hp-pcentre";
    /**
     * Constructs a {@link SauceOnDemandAuthentication} instance using the authentication
     * supplied by environment variables or from an external file, use the no-arg {@link SauceOnDemandAuthentication} constructor.
     * The environment variable needs to be set on the local machine OR on the Jenkins job so that the server running the job has access to it
     * Alternatively if the .on-demand file is being use then this needs to have the details set
     */

    public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();
    /**
     * JUnit Rule which will mark the Sauce Job as passed/failed when the test succeeds or fails.
     */
    @Rule
    public SauceOnDemandTestWatcher resultReportingTestWatcher = new SauceOnDemandTestWatcher(this, authentication);
    //rule that allows the test class to get the method name
    @Rule
    public TestName name = new TestName() {
        public String getMethodName() {

            return String.format("%s", super.getMethodName());
        }
    };
    //logging has been set to false as default - tests will need to set them to true using the logEnabled function
    protected boolean logFlag = false;
    protected String browser;
    protected String os;
    protected String version;
    protected String deviceName;
    protected String deviceOrientation;
    protected String sessionId;
    protected static WebDriver driver;
    String authPassword = System.getenv("testingservices_hpservice_password");
    public SauceLabs(String os, String version, String browser, String deviceName, String deviceOrientation) {
        super();
        this.os = os;
        this.version = version;
        this.browser = browser;
        this.deviceName = deviceName;
        this.deviceOrientation = deviceOrientation;
        //devicename and deviceorientation only required for mobile devices
    }

    public SauceLabs() {

    }


    /**
     * @return a LinkedList containing String arrays representing the browser combinations the test should be run against. The values
     * in the String array are used as part of the invocation of the test constructor
     */
    @ConcurrentParameterized.Parameters
    public static LinkedList browsersStrings() {
        LinkedList browsers = new LinkedList();

        browsers.add(new String[]{"Windows 10", "67", "chrome", null, null});
        browsers.add(new String[]{"Windows 7", "67", "chrome", null, null});
        return browsers;
    }


    @BeforeClass
    public static void setupClass() {
        //Old US Data Center URL
        //seleniumURI = "@ondemand.saucelabs.com:443";
        //EU Data Center URL
        seleniumURI = "@ondemand.eu-central-1.saucelabs.com";
        System.out.println("fwkLog:selenium URI is: " + seleniumURI);

        buildTag = System.getenv("BUILD_TAG");
        if (buildTag == null) {
            buildTag = System.getenv("SAUCE_BUILD_NAME");
        }
    }

    //test class can set this to true to allow logging to be activated so that the log file is generated and results go to splunk
    public void logEnabled(boolean logFlagStatus) {
        logFlag = logFlagStatus;
        System.out.println("fwkLog:The log flag has been set to: " + logFlagStatus);
    }

    /**
     * Constructs a new {@link RemoteWebDriver} instance which is configured to use the capabilities defined by the {@link #browser},
     * {@link #version} and {@link #os} instance variables, and which is configured to run against ondemand.saucelabs.com, using
     * the username and access key populated by the {@link #authentication} instance.
     *
     * @throws Exception if an error occurs during the creation of the {@link RemoteWebDriver} instance.
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("fwkLog:Test setup starting.");
        //Resetting the username and accesskey variables in case ENV variables did not work and .sauce-ondemand file in home directory was used to connect
        username = authentication.getUsername();
        accesskey = authentication.getAccessKey();
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability(CapabilityType.BROWSER_NAME, browser);
        capabilities.setCapability(CapabilityType.VERSION, version);
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("device-orientation", deviceOrientation);
        capabilities.setCapability("platform", os);
        capabilities.setCapability("extendedDebugging", true);
        //capabilities.setCapability("tunnelIdentifier", "templateFramework");
        String methodName = sessionId;
        capabilities.setCapability("name", methodName);

        //Using the Jenkins ENV var. You can use your own. If it is not set test will run without a build id.
        if (buildTag != null) {
            capabilities.setCapability("build", buildTag);
        }



        //uses proxy credentials of hp service account (this may not be required after sauce connect is added)
        Authenticator.setDefault(new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(authUser, authPassword.toCharArray());
            }
        });

        //proxy settings
        System.out.println("fwkLog:External variable is set to: " + external);
        if (external == null) {
            System.out.println("Setting the BP proxy details as BP_EXTERNAL is null");
            System.getProperties().put("http.proxyHost", "emeaproxy.bp.com");
            System.getProperties().put("http.proxyPort", "80");
            System.getProperties().put("https.proxyHost", "emeaproxy.bp.com");
            System.getProperties().put("https.proxyPort", "80");
            System.setProperty("http.proxyUser", authUser);
            System.setProperty("http.proxyPassword", authPassword);
        }
        System.out.println("fwkLog:Attempting to load the web driver with the capabilities");
        this.driver = new RemoteWebDriver(new URL("https://" + username + ":" + accesskey + seleniumURI + "/wd/hub"), capabilities);
        System.out.println("fwkLog:Remote web driver started.");
        this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
        System.out.println("fwkLog:SessionID: " + sessionId);
        ((JavascriptExecutor) driver).executeScript("sauce:job-name=" + getClass().getName());
        System.out.println("fwkLog:SauceOnDemandSessionID=" + sessionId+ " job-name="+getClass().getName());
    }

    public void SauceLabs(WebDriver driver) {
        this.driver = driver;
        //This initElements method will create all WebElements
        PageFactory.initElements(driver, this);
        //dummy = this;

    }

    public WebDriver getWebDriver() {
        return driver;
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("fwkLog:Terminating the driver");
        driver.quit();
        System.out.println("fwkLog:The driver has been terminated");

    }

    /**
     * @return the value of the Sauce Job id.
     */
    @Override
    public String getSessionId() {
        return sessionId;
    }
}
