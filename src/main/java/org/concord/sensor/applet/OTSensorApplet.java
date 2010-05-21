package org.concord.sensor.applet;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.concord.framework.data.stream.DataListener;
import org.concord.framework.otrunk.OTControllerService;
import org.concord.otrunk.applet.OTAppletViewer;
import org.concord.sensor.state.OTSensorDataProxy;
import org.concord.sensor.state.SensorDataProxy;

/**
 * This applet is an extension of the OTAppletViewer class. It is intended to be used specifically for
 * reading sensor data and passing that off to a listener. Listeners can be created in Java or Javascript.
 * 
 * Basic usage:
 * 1) Create a javascript listener that implements the org.concord.sensor.applet.JavascriptDataListener interface
 * 2a) set the applet param "listenerPath" to the string object name for your listener
 *   '<param name="listenerPath" value="jsListener" />'
 *   OR
 * 2b) create the listener bridge through the applet and register it in the applet
 *   var bridge = applet.createJavascriptBridge("jsListener");
 *   applet.addDataListener(bridge);
 *   
 * @author aunger
 *
 */
public class OTSensorApplet extends OTAppletViewer {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OTSensorApplet.class.getName());
    private SensorDataProxy sensorProxy;

    private JPanel mainPanel;
    private JButton startButton;
    private JButton stopButton;
    
    private DefaultDataListener defaultListener = new DefaultDataListener();

    @Override
    public void init() {
        try {
            URL nativeJarUrl = new URL(getCodeBase(), getNativeJarName());
            NativeLibraryHandler handler = new NativeLibraryHandler(nativeJarUrl);
            handler.initializeLibrary();
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Native sensor lib url invalid!", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't create temp files to hold native library!", e);
        }

        super.init();
    }

    private String getNativeJarName() {
        if(System.getProperty("os.name").startsWith("Windows")) {
            return "vernier-goio-win32-nar.jar";
        }else if(System.getProperty("os.name").startsWith("Mac")) {
            return "vernier-goio-macosx-nar.jar";
        }
        return "vernier-goio-macosx-nar.jar";
    }

    protected void initDataProxy() throws Exception {
        OTSensorDataProxy otSensorProxy = (OTSensorDataProxy) getOTrunk().getRoot();
        OTControllerService controllerService = otSensorProxy.getOTObjectService().createControllerService();
        sensorProxy = (SensorDataProxy) controllerService.getRealObject(otSensorProxy);

        if (getParameter("listenerPath") != null) {
            DataListener jsListener = createJavascriptBridge(getParameter("listenerPath"));
            addDataListener(jsListener);
        } else {
            addDataListener(defaultListener);
        }
    }

    public void startCollecting() {
        sensorProxy.start();
    }

    public void stopCollecting() {
        sensorProxy.stop();
    }

    @Override
    public void setupView() {
        try {
            System.out.println("Starting to set up view.");
            initDataProxy();
            System.out.println("Finished init'ing sensor proxy");
            mainPanel = new JPanel(new FlowLayout());
            startButton = new JButton("Start");
            stopButton = new JButton("Stop");
            mainPanel.add(startButton);
            mainPanel.add(stopButton);

            getContentPane().add(mainPanel);

            startButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startCollecting();
                }
            });

            stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopCollecting();
                }
            });
            System.out.println("Finished view setup.");
        } catch (Exception e1) {
            logger.log(Level.SEVERE, "Failed to set up sensor proxy!", e1);
            getContentPane().add(new JLabel("Setup failure."));
        }
        
        getContentPane().validate();
        getContentPane().repaint();
    }

    @Override
    public void destroy() {
        if (sensorProxy != null) {
            sensorProxy.stop();
            sensorProxy.removeDataListener(defaultListener);
            sensorProxy = null;
        }
        super.destroy();
    }
    
    public void addDataListener(DataListener listener) {
        sensorProxy.addDataListener(listener);
    }
    
    public void removeDataListener(DataListener listener) {
        sensorProxy.removeDataListener(listener);
    }
    
    public JavascriptDataBridge createJavascriptBridge(String jsObjectPath) {
        return new JavascriptDataBridge(jsObjectPath, this);
    }
}
