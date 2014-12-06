package findMyIP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class IPUtils {

    private static String[] urls = { "http://myexternalip.com/raw",
                                     "http://ipchk.sourceforge.net/rawip/" };

    private static final URL IP_CHECK_URL =
            makeURL(urls);

    private static URL makeURL(String[] URLStrings) {

        for (String url : URLStrings) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                // Try next url.
            }
        }

        return null;
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                IP_CHECK_URL.openStream()));) {
            String ipString = in.readLine();
            if (validIP(ipString)) {
                return ipString;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getIPFromAPI() {
        try {
            InetAddress IP = InetAddress.getLocalHost();
            return IP.getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean validIP(String IPAddress) {
        return IPAddress.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
    }
}
