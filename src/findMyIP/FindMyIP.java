package findMyIP;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FindMyIP {
  
  private static final URL IP_CHECK_URL =
      makeURL("http://ipchk.sourceforge.net/rawip/");
  
  private static final int QUICK_INTERVAL = 5 * 1000;
  private static final int SLOW_INTERVAL  = 5 * 60 * 1000;
  
  private static final String name = "FindMyIP";
  
  private String currentIP;
  private TrayIcon trayIcon;
  private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
  
  public FindMyIP() {
    currentIP = getIPAddress();
  }
  
  private static URL makeURL(String URLString) {
    
    try {
      
      return new URL(URLString);
      
    } catch (MalformedURLException e) {
      
      e.printStackTrace();
      return null;
      
    }
    
  }
  
  private String getIPAddress() {
    
    try (BufferedReader in = new BufferedReader(new InputStreamReader(
        IP_CHECK_URL.openStream()));) {
      
      return in.readLine();
      
    } catch (Exception e) {
      
      updateTimer(QUICK_INTERVAL);
      return "<unknown>";
      
    }
     
  }
  
  private void displayMessage(String message, TrayIcon.MessageType messageType) {
    trayIcon.displayMessage(name, message + currentIP, messageType);
  }
  
  private void displayUpdateMessage(boolean onlyWhenChanged) {
    
    String newIP = getIPAddress();
    
    if (!currentIP.equals(newIP)) {
      
      currentIP = newIP;
      displayMessage("New IP: ", TrayIcon.MessageType.INFO);
      
      if (timer != null && !currentIP.equals("<unknown>")) {
        updateTimer(SLOW_INTERVAL);
      }
      
    } else if (!onlyWhenChanged)
      displayMessage("IP not changed: ", TrayIcon.MessageType.INFO);
    
  }
  
  private void copyIPToClipboard() {
    
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        new StringSelection(currentIP), null);
    
  }
  
  private void createAndShowGUI() {
    
    if (!SystemTray.isSupported()) {
      
        System.out.println("SystemTray is not supported");
        System.exit(1);
        
    }
    
    PopupMenu popupMenu= new PopupMenu();
    
    MenuItem copyItem = new MenuItem("Copy IP to clipboard");
    MenuItem updateItem = new MenuItem("Update now");
    MenuItem exitItem = new MenuItem("Exit");
    
    popupMenu.add(copyItem);
    popupMenu.addSeparator();
    popupMenu.add(updateItem);
    popupMenu.add(exitItem);

    try {
      trayIcon = new TrayIcon(ImageIO.read(getClass().getResource(("/icon.png"))), name, popupMenu);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    
    try {
      SystemTray.getSystemTray().add(trayIcon);
    } catch (AWTException e) {
      e.printStackTrace();
    }
    
    trayIcon.setImageAutoSize(true);
    trayIcon.setToolTip("Current IP: " + currentIP);
    trayIcon.displayMessage(name, "Current IP: " + currentIP,
        TrayIcon.MessageType.INFO);
    
    trayIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
          if (e.getButton() == MouseEvent.BUTTON1) {
              copyIPToClipboard();
          }
      }
    });
    
    copyItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        copyIPToClipboard();
      }
    });
    
    updateItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        displayUpdateMessage(false);
      }
    });
    
    exitItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          SystemTray.getSystemTray().remove(trayIcon);
          System.exit(0);
      }
    });
  }
  
  private void updateTimer(int intervalTime) {

    timer.shutdown();
    timer = Executors.newSingleThreadScheduledExecutor();
    timer.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
            displayUpdateMessage(true);
        }
    }, 0, intervalTime, TimeUnit.SECONDS);
    
  }

  public static void main(String[] args) {
    
    final FindMyIP ipFinder = new FindMyIP();  
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ipFinder.createAndShowGUI();
        ipFinder.updateTimer(SLOW_INTERVAL);
      }
    });
    
  }

}
