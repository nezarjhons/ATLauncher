/**
 * Copyright 2013 by ATLauncher and Contributors
 *
 * ATLauncher is licensed under CC BY-NC-ND 3.0 which allows others you to
 * share this software with others as long as you credit us by linking to our
 * website at http://www.atlauncher.com. You also cannot modify the application
 * in any way or make commercial use of this software.
 *
 * Link to license: http://creativecommons.org/licenses/by-nc-nd/3.0/
 */
package com.atlauncher.data;

import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlauncher.exceptions.InvalidLanguage;
import com.atlauncher.exceptions.InvalidPack;
import com.atlauncher.exceptions.InvalidRam;
import com.atlauncher.exceptions.InvalidServer;
import com.atlauncher.exceptions.InvalidWindowHeight;
import com.atlauncher.exceptions.InvalidWindowWidth;
import com.atlauncher.gui.InstancesPanel;
import com.atlauncher.gui.LauncherConsole;
import com.atlauncher.gui.Utils;

/**
 * Settings class for storing all data for the Launcher and the settings of the user
 * 
 * @author Ryan
 */
public class Settings {

    // Users Settings
    private Properties properties; // Properties to store everything in
    private File propertiesFile = new File("ATLauncher.conf"); // File for properties
    private Language language; // Language for the Launcher
    private Server server; // Server to use for the Launcher
    private int ram; // RAM to use when launching Minecraft
    private int windowWidth; // Width of the Minecraft window
    private int windowHeight; // Height of the Minecraft window
    private String javaParamaters; // Extra Java paramaters when launching Minecraft
    private boolean enableConsole; // If to show the console by default
    private boolean enableLeaderboards; // If to enable the leaderboards
    private boolean enableLogs; // If to enable logs

    // Packs, Addons and Instances
    private ArrayList<Pack> packs = new ArrayList<Pack>(); // Packs in the Launcher
    private ArrayList<Instance> instances = new ArrayList<Instance>(); // Users Installed Instances
    private ArrayList<Addon> addons = new ArrayList<Addon>(); // Addons in the Launcher

    // Launcher Settings
    private JFrame parent; // Parent JFrame of the actual Launcher
    private LauncherConsole console; // The Launcher's Console
    private ArrayList<Language> languages = new ArrayList<Language>(); // Languages for the Launcher
    private ArrayList<Server> servers = new ArrayList<Server>(); // Servers for the Launcher
    private InstancesPanel instancesPanel; // The instances panel
    private boolean firstTimeRun = false; // If this is the first time the Launcher has been run
    private Server bestConnectedServer; // The best connected server for Auto selection
    private boolean offlineMode = false; // If offline mode is enabled

    public Settings() {
        this.console = new LauncherConsole();
        this.properties = new Properties(); // Make the properties variable
    }

    public void loadEverything() {
        setupServers(); // Setup the servers available to use in the Launcher
        testServers(); // Test servers for best connected one
        loadServerProperty(); // Get users Server preference
        loadLanguages(); // Load the Languages available in the Launcher
        loadPacks(); // Load the Packs available in the Launcher
        loadAddons(); // Load the Addons available in the Launcher
        loadProperties(); // Load the users Properties
    }

    /**
     * Sets the main parent JFrame reference for the Launcher
     * 
     * @param parent
     *            The Launcher main JFrame
     */
    public void setParentFrame(JFrame parent) {
        this.parent = parent;
    }

    /**
     * Load the users Server preference from file
     */
    public void loadServerProperty() {
        try {
            if (!propertiesFile.exists()) {
                propertiesFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.properties.load(new FileInputStream(propertiesFile));
            this.server = getServerByName(properties.getProperty("server", "Auto"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidServer e) {
            console.log(e.getMessage());
            this.server = this.servers.get(0); // Server not found, use default of Auto
        }
    }

    /**
     * Load the properties from file
     */
    public void loadProperties() {
        try {
            this.properties.load(new FileInputStream(propertiesFile));
            this.firstTimeRun = Boolean
                    .parseBoolean(properties.getProperty("firsttimerun", "true"));
            this.language = getLanguageByName(properties.getProperty("language", "English"));
            this.server = getServerByName(properties.getProperty("server", "Auto"));
            this.ram = Integer.parseInt(properties.getProperty("ram", "512"));
            if (this.ram > Utils.getMaximumRam()) {
                throw new InvalidRam("Cannot allocate " + this.ram + "MB of Ram");
            }
            this.windowWidth = Integer.parseInt(properties.getProperty("windowwidth", "854"));
            if (this.windowWidth > Utils.getMaximumWindowWidth()) {
                throw new InvalidWindowWidth("Cannot set screen width to " + this.windowWidth);
            }
            this.windowHeight = Integer.parseInt(properties.getProperty("windowheight", "854"));
            if (this.windowHeight > Utils.getMaximumWindowHeight()) {
                throw new InvalidWindowHeight("Cannot set screen height to " + this.windowHeight);
            }
            this.javaParamaters = properties.getProperty("javaparameters", "");
            this.enableConsole = Boolean.parseBoolean(properties.getProperty("enableconsole",
                    "true"));
            this.enableLeaderboards = Boolean.parseBoolean(properties.getProperty(
                    "enableleaderboards", "true"));
            this.enableLogs = Boolean.parseBoolean(properties.getProperty("enablelogs", "true"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidLanguage e) {
            console.log(e.getMessage());
            this.language = languages.get(0); // Language not found, use the default first one
        } catch (InvalidServer e) {
            console.log(e.getMessage());
            this.server = bestConnectedServer; // Server not found, use the best one found
        } catch (InvalidRam e) {
            console.log(e.getMessage());
            this.ram = 512; // User tried to allocate too much ram, set it back to 0.5GB
        } catch (InvalidWindowWidth e) {
            console.log(e.getMessage());
            this.windowWidth = 854; // User tried to make screen size wider than they have
        } catch (InvalidWindowHeight e) {
            console.log(e.getMessage());
            this.windowHeight = 480; // User tried to make screen size wider than they have
        }
    }

    /**
     * Save the properties to file
     */
    public void saveProperties() {
        try {
            properties.setProperty("firsttimerun", "false");
            properties.setProperty("language", this.language.getName());
            properties.setProperty("server", this.server.getName());
            properties.setProperty("ram", this.ram + "");
            properties.setProperty("windowwidth", this.windowWidth + "");
            properties.setProperty("windowheight", this.windowHeight + "");
            properties.setProperty("javaparameters", this.javaParamaters);
            properties.setProperty("enableconsole", (this.enableConsole) ? "true" : "false");
            properties.setProperty("enableleaderboards", (this.enableLeaderboards) ? "true"
                    : "false");
            properties.setProperty("enablelogs", (this.enableLogs) ? "true" : "false");
            this.properties.store(new FileOutputStream(propertiesFile), "ATLauncher Settings");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The servers available to use in the Launcher
     * 
     * These MUST be hardcoded in order for the Launcher to make the initial connections to download
     * files
     */
    private void setupServers() {
        servers.add(new Server("Auto", ""));
        servers.add(new Server("Europe", "eu.atlcdn.net"));
        servers.add(new Server("US East", "useast.atlcdn.net"));
        servers.add(new Server("US West", "uswest.atlcdn.net"));
    }

    /**
     * Tests the servers for availability and best connection
     */
    private void testServers() {
        double[] responseTimes = new double[servers.size()];
        int count = 0;
        int up = 0;
        for (Server server : servers) {
            if (server.isAuto())
                continue; // Don't scan the Auto server
            double startTime = System.currentTimeMillis();
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(server.getTestURL())
                        .openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(3000);
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    responseTimes[count] = 1000000.0;
                    console.log("Server " + server.getName() + " isn't available!");
                    server.disableServer();
                } else {
                    double endTime = System.currentTimeMillis();
                    responseTimes[count] = endTime - startTime;
                    console.log("Server " + server.getName() + " is available! ("
                            + responseTimes[count] + ")");
                    up++;
                }
            } catch (SocketTimeoutException e) {
                responseTimes[count] = 1000000.0;
                console.log("Server " + server.getName() + " isn't available!");
                server.disableServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
            count++;
        }
        int best = 0;
        double bestTime = 10000000.0;
        for (int i = 0; i < responseTimes.length; i++) {
            if (responseTimes[i] < bestTime) {
                best = i;
                bestTime = responseTimes[i];
            }
        }
        if (up != 0) {
            console.log("The best connected server is " + servers.get(best).getName());
            this.bestConnectedServer = servers.get(best);
        } else {
            JOptionPane.showMessageDialog(null,
                    "<html><center>There was an issue connecting to ATLauncher "
                            + "Servers<br/><br/>Offline mode is now enabled.<br/><br/>"
                            + "To install packs again, please try connecting later"
                            + "</center></html>", "Error Connecting To ATLauncher Servers",
                    JOptionPane.ERROR_MESSAGE);
            this.offlineMode = true; // Set offline mode to be true
        }
    }

    /**
     * Loads the languages for use in the Launcher
     */
    private void loadLanguages() {
        Language language;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(getFileURL("launcher/languages.xml"));
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("language");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String name = element.getAttribute("name");
                    String localizedName = element.getAttribute("localizedname");
                    String file = element.getAttribute("file");
                    String author = element.getAttribute("author");
                    language = new Language(name, localizedName, file, author);
                    languages.add(language);
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the Packs for use in the Launcher
     */
    private void loadPacks() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(getFileURL("launcher/packs.xml"));
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("pack");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int id = Integer.parseInt(element.getAttribute("id"));
                    String name = element.getAttribute("name");
                    Version[] versions;
                    if (element.getAttribute("versions").isEmpty()) {
                        // Pack has no versions so log it and continue to next pack
                        getConsole().log("Pack " + name + " has no versions!");
                        continue;
                    } else {
                        String[] tempVersions = element.getAttribute("versions").split(",");
                        versions = new Version[tempVersions.length];
                        for (int v = 0; v < tempVersions.length; v++) {
                            String[] parsed = tempVersions[v].split("\\.");
                            versions[v] = new Version(Integer.parseInt(parsed[0]),
                                    Integer.parseInt(parsed[1]), Integer.parseInt(parsed[2]));
                        }
                    }
                    Version[] minecraftversions;
                    if (element.getAttribute("minecraftversions").isEmpty()) {
                        // Pack has no versions so log it and continue to next pack
                        getConsole().log("Pack " + name + " has no minecraftversions!");
                        continue;
                    } else {
                        String[] tempVersions = element.getAttribute("minecraftversions")
                                .split(",");
                        minecraftversions = new Version[tempVersions.length];
                        for (int mv = 0; mv < tempVersions.length; mv++) {
                            String[] parsed = tempVersions[mv].split("\\.");
                            minecraftversions[mv] = new Version(Integer.parseInt(parsed[0]),
                                    Integer.parseInt(parsed[1]), Integer.parseInt(parsed[2]));
                        }
                    }
                    String description = element.getAttribute("description");
                    Pack pack = new Pack(id, name, versions, minecraftversions, description);
                    packs.add(pack);
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the Addons for use in the Launcher
     */
    private void loadAddons() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(getFileURL("launcher/addons.xml"));
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("addon");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int id = Integer.parseInt(element.getAttribute("id"));
                    String name = element.getAttribute("name");
                    Version[] versions;
                    if (element.getAttribute("versions").isEmpty()) {
                        // Pack has no versions so log it and continue to next
                        // pack
                        getConsole().log("Addon " + name + " has no versions!");
                        continue;
                    } else {
                        String[] tempVersions = element.getAttribute("versions").split(",");
                        versions = new Version[tempVersions.length];
                        for (int v = 0; v < tempVersions.length; v++) {
                            String[] parsed = tempVersions[v].split("\\.");
                            versions[v] = new Version(Integer.parseInt(parsed[0]),
                                    Integer.parseInt(parsed[1]), Integer.parseInt(parsed[2]));
                        }
                    }
                    String description = element.getAttribute("description");
                    Pack forPack;
                    Pack pack = getPackByID(id);
                    if (pack != null) {
                        forPack = pack;
                    } else {
                        getConsole().log("Addon " + name + " is not available for any packs!");
                        continue;
                    }
                    Addon addon = new Addon(id, name, versions, description, forPack);
                    addons.add(addon);
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidPack e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds out if this is the first time the Launcher has been run
     * 
     * @return true if the Launcher hasn't been run and setup yet, false for otherwise
     */
    public boolean isFirstTimeRun() {
        return this.firstTimeRun;
    }

    /**
     * Get the Packs available in the Launcher
     * 
     * @return The Packs available in the Launcher
     */
    public ArrayList<Pack> getPacks() {
        return this.packs;
    }

    /**
     * Get the Instances available in the Launcher
     * 
     * @return The Instances available in the Launcher
     */
    public ArrayList<Instance> getInstances() {
        return this.instances;
    }

    /**
     * Get the Addons available in the Launcher
     * 
     * @return The Addons available in the Launcher
     */
    public ArrayList<Addon> getAddons() {
        return this.addons;
    }

    /**
     * Get the Languages available in the Launcher
     * 
     * @return The Languages available in the Launcher
     */
    public ArrayList<Language> getLanguages() {
        return this.languages;
    }

    /**
     * Get the Servers available in the Launcher
     * 
     * @return The Servers available in the Launcher
     */
    public ArrayList<Server> getServers() {
        return this.servers;
    }

    /**
     * Determines if offline mode is enabled or not
     * 
     * @return true if offline mode is enabled, false otherwise
     */
    public boolean isInOfflineMode() {
        return this.offlineMode;
    }

    /**
     * Returns the JFrame reference of the main Launcher
     * 
     * @return Main JFrame of the Launcher
     */
    public Window getParent() {
        return this.parent;
    }

    /**
     * Sets the Panel used for Instances
     * 
     * @param instancesPanel
     *            Instances Panel
     */
    public void setInstancesPanel(InstancesPanel instancesPanel) {
        this.instancesPanel = instancesPanel;
    }

    /**
     * Reloads the Instances Panel table
     */
    public void reloadTable() {
        this.instancesPanel.reloadTable();
    }

    /**
     * Checks to see if there is already an instance with the name provided or not
     * 
     * @param name
     *            The name of the instance to check for
     * @return True if there is an instance with the same name already
     */
    public boolean isInstance(String name) {
        for (Instance instance : instances) {
            if (instance.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a Pack from the given ID number
     * 
     * @param id
     *            ID of the Pack to find
     * @return Pack if the pack is found from the ID
     * @throws InvalidPack
     *             If ID is not found
     */
    public Pack getPackByID(int id) throws InvalidPack {
        for (Pack pack : packs) {
            if (pack.getID() == id) {
                return pack;
            }
        }
        throw new InvalidPack("No pack exists with ID " + id);
    }

    /**
     * Finds a Language from the given name
     * 
     * @param name
     *            Name of the Language to find
     * @return Language if the language is found from the name
     * @throws InvalidLanguage
     *             If Language is not found
     */
    private Language getLanguageByName(String name) throws InvalidLanguage {
        for (Language language : languages) {
            if (language.getName().equalsIgnoreCase(name)) {
                return language;
            }
        }
        throw new InvalidLanguage("No language exists with name " + name);
    }

    /**
     * Finds a Language from the given name
     * 
     * @param name
     *            Name of the Language to find
     * @return Language if the language is found from the name
     * @throws InvalidLanguage
     *             If Language is not found
     */
    private Server getServerByName(String name) throws InvalidServer {
        for (Server server : servers) {
            if (server.getName().equalsIgnoreCase(name)) {
                return server;
            }
        }
        throw new InvalidServer("No server exists with name " + name);
    }

    /**
     * Gets the URL for a file on the user selected server
     * 
     * @param filename
     *            Filename including directories on the server
     * @return URL of the file
     */
    public String getFileURL(String filename) {
        return this.server.getFileURL(filename, bestConnectedServer);
    }

    /**
     * Gets the Launcher's current Console instance
     * 
     * @return The Launcher's Console instance
     */
    public LauncherConsole getConsole() {
        return this.console;
    }

    /**
     * Returns the best connected server
     * 
     * @return The server that the user was best connected to
     */
    public Server getBestConnectedServer() {
        System.out.println("hi");
        return this.bestConnectedServer;
    }

    /**
     * Gets the users current active Language
     * 
     * @return The users set language
     */
    public Language getLanguage() {
        return this.language;
    }

    /**
     * Sets the users current active Language
     * 
     * @param language
     *            The language to set to
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * Gets the users current active Server
     * 
     * @return The users set server
     */
    public Server getServer() {
        return this.server;
    }

    /**
     * Sets the users current active Server
     * 
     * @param server
     *            The server to set to
     */
    public void setServer(Server server) {
        this.server = server;
    }

}