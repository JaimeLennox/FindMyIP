package findMyIP;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class FindMyIP {
  
    private static final int FAST_INTERVAL = 5;       // 5 seconds.
    private static final int SLOW_INTERVAL = 5 * 60; // 5 minutes.

    private static final Preferences prefs = Preferences.userNodeForPackage(FindMyIP.class);
    private static final String DEFAULT_FILENAME = "";
    private static final String SAVE_FILE = "save";
    private static final String NAME = "FindMyIP";

    private String currentIP;
    private String currentLocalIP;
    private TrayIcon trayIcon;

    private ScheduledExecutorService slowTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService fastTimer = Executors.newSingleThreadScheduledExecutor();
    private Turn turn = Turn.SLOW;

    private boolean daemon;
  
    public FindMyIP(boolean daemon, String saveFile) {
        this.daemon = daemon;
        if (saveFile != null) prefs.put(SAVE_FILE, saveFile);
        currentIP = IPUtils.getIPAddress();
        currentLocalIP = IPUtils.getLocalIPAddress();
        System.out.println("Started with current IP " + currentIP + ", local IP " + currentLocalIP);
    }

    private File getSaveFile() {
        String saveFileName = prefs.get(SAVE_FILE, DEFAULT_FILENAME);
        return saveFileName.equals(DEFAULT_FILENAME) ? null : new File(saveFileName);
    }

    private void displayMessage(String message, String localMessage) {
        System.out.println("Current IP: " + currentIP);
        System.out.println("Current local IP: " + currentLocalIP);
        trayIcon.displayMessage(NAME, message + currentIP + "\n" + localMessage + currentLocalIP, TrayIcon.MessageType.INFO);
    }

    private void update(boolean displayMessage) {
        System.out.println("Updating");
        String message;
        String localMessage;
        boolean save = false;
        String newIP = IPUtils.getIPAddress();

        if (!currentIP.equals(newIP)) {
            System.out.println("New IP found");
            currentIP = newIP;
            message = "New public IP: ";
            save = true;
        }
        else {
            message = "IP not changed: ";
        }

        newIP = IPUtils.getLocalIPAddress();

        if (!currentLocalIP.equals(newIP)) {
            System.out.println("New local IP found");
            currentLocalIP = newIP;
            localMessage = "New local IP: ";
            save = true;
        }
        else {
            localMessage = "Local IP not changed: ";
        }

        if (save) save();

        turn = currentIP.equals("<unknown>") || currentLocalIP.equals("<unknown>") ? Turn.FAST : Turn.SLOW;

        if (displayMessage || save) displayMessage(message, localMessage);
        trayIcon.setToolTip("Current IP: " + currentIP + "\n" + "Current Local IP: " + currentLocalIP);
    }

    private void save() {
        File saveFile = getSaveFile();
        if (saveFile == null) return;
        try {
            System.out.println("Saving to file: " + saveFile.getAbsolutePath());
            PrintWriter printWriter = new PrintWriter(saveFile);
            printWriter.println(currentIP);
            printWriter.println(currentLocalIP);
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void copyIPToClipboard() {
        System.out.println("Copied IP to clipboard");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(currentIP), null);

    }

    private void copyLocalIPToClipboard() {
        System.out.println("Copied local IP to clipboard");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(currentLocalIP), null);
    }

    private void start() {
        System.out.println("Starting application");

        if (daemon) {
            System.out.println("Application started in daemon mode");
            updateTimers();
            return;
        }
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            System.exit(1);
        }

        final PopupMenu popupMenu= new PopupMenu();

        MenuItem copyItem = new MenuItem("Copy public IP to clipboard");
        MenuItem copyLocalItem = new MenuItem("Copy local IP to clipboard");
        final MenuItem saveItem = new MenuItem(getSaveFile() == null ? "Save to file..." : "Cancel save to file");
        MenuItem updateItem = new MenuItem("Update now");
        MenuItem exitItem = new MenuItem("Exit");

        popupMenu.add(copyItem);
        popupMenu.add(copyLocalItem);
        popupMenu.addSeparator();
        popupMenu.add(updateItem);
        popupMenu.add(saveItem);
        popupMenu.add(exitItem);

        try {
            trayIcon = new TrayIcon(ImageIO.read(getClass().getResource(("/icon.png"))), NAME, popupMenu);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            SystemTray.getSystemTray().add(trayIcon);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }

        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Current IP: " + currentIP + "\n" + "Current Local IP: " + currentLocalIP);
        displayMessage("Current IP: ", "Current Local IP: ");

        copyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyIPToClipboard();
            }
        });

        copyLocalItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyLocalIPToClipboard();
            }
        });

        updateItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update(true);
            }
        });

        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getSaveFile() == null) {
                JFileChooser fileChooser = new JFileChooser();
                int chosenOption = fileChooser.showSaveDialog(null);
                if (chosenOption == JFileChooser.APPROVE_OPTION) {
                    prefs.put(SAVE_FILE, fileChooser.getSelectedFile().getAbsolutePath());
                    saveItem.setLabel("Cancel save to file");
                    save();
                }

                }
                else {
                    prefs.put(SAVE_FILE, DEFAULT_FILENAME);
                    saveItem.setLabel("Save to file...");
                }
            }
        });

        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SystemTray.getSystemTray().remove(trayIcon);
                System.exit(0);
            }
        });

        System.out.println("Finished setup, starting timers");

        updateTimers();
    }

    private void updateTimers() {
        slowTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (turn == Turn.FAST) return;
                update(false);
            }
        }, 0, SLOW_INTERVAL, TimeUnit.SECONDS);

        fastTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (turn == Turn.SLOW) return;
                update(false);
            }
        }, 0, FAST_INTERVAL, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {

        boolean daemon = false;
        String file = null;

        if (args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-d")) {
                    daemon = true;
                }
                else if (args[i].equals("-f")) {
                    if (i + 1 >= args.length) {
                        System.out.println("Please provide a file to save to");
                    }
                    else {
                        file = args[i+1];
                    }

                }
            }
        }

        final FindMyIP ipFinder = new FindMyIP(daemon, file);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ipFinder.start();
            }
        });
    }

}
