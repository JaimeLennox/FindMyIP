package findMyIP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPUtils {

    private static Logger logger = LoggerFactory.getLogger(IPUtils.class);

    private static String[] urls = { "http://myexternalip.com/raw",
                                     "http://ipchk.sourceforge.net/rawip/" };

    private static List<URL> validUrls = makeURLs(urls);

    private static List<URL> makeURLs(String[] URLStrings) {
        List<URL> urls = new ArrayList<>();

        for (String url : URLStrings) {
            try {
                urls.add(new URL(url));
            } catch (MalformedURLException e) {
                logger.error("Url " + url + " is not valid.", e);
            }
        }

        return urls;
    }

    public static String getIPAddress() {
        return checkIP(getIPFromURL());
    }

    public static String getLocalIPAddress() {
        return checkIP(getIPFromAPI());
    }

    private static String checkIP(String IPAddress) {
        if (IPAddress == null || IPAddress.equals("127.0.0.1")) {
            return "<unknown>";
        }

        return IPAddress;
    }

    private static String getIPFromURL() {
        for (URL url : validUrls) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String ipString = in.readLine();
                if (validIP(ipString)) {
                    return ipString;
                }
            } catch (Exception e) {
                logger.error("Couldn't retrieve IP from url " + url, e);
            }
        }

        return null;
    }

    private static String getIPFromAPI() {
        try {
            InetAddress IP = InetAddress.getLocalHost();
            return IP.getHostAddress();
        }
        catch (UnknownHostException e) {
            logger.error("Couldn't retrieve IP from localhost", e);
            return null;
        }
    }

    private static boolean validIP(String IPAddress) {
        return IPAddress.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
    }
}
