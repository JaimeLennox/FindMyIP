package findMyIP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class IPUtils {

    private static final URL IP_CHECK_URL =
            makeURL("http://ipchk.sourceforge.net/rawip/");

    private static URL makeURL(String URLString) {

        try {
            return new URL(URLString);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getIPAddress() {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                IP_CHECK_URL.openStream()));) {
            return in.readLine();
        }
        catch (Exception e) {
            return "<unknown>";
        }
    }
}
