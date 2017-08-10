/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.basic;


import net.cp.mtk.j2me.tools.discovery.Logger;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class SystemProperties
{
    //the properties we generally expect to be present
    private static final String[] PROPERTIES_CHECK = { "microedition.profiles", 
                                                       "microedition.configuration",
                                                       "microedition.locale", 
                                                       "microedition.platform",
                                                       "microedition.encoding",
                                                       "microedition.io.file.FileConnection.version",
                                                       "microedition.pim.version",
                                                       "fileconn.dir.photos", 
                                                       "fileconn.dir.videos",
                                                       "fileconn.dir.music", 
                                                       "fileconn.dir.memorycard", 
                                                       "file.separator" 
                                                     };

    //other properties to log
    private static final String[] PROPERTIES_CLDC = { "microedition.profiles", 
                                                      "microedition.configuration",
                                                      "microedition.locale", 
                                                      "microedition.timezone",
                                                      "default.timezone",
                                                      "microedition.platform",
                                                      "microedition.encoding", 
                                                      "microedition.commports",
                                                      "commports.maxbaudrate",
                                                      "microedition.hostname",
                                                      "microedition.protocolpath",
                                                      "microedition.jtwi.version",
                                                      "microedition.msa.version" 
                                                    };
    
    private static final String[] PROPERTIES_MMAPI = { "microedition.media.version",
                                                       "supports.mixing", 
                                                       "supports.audio.capture",
                                                       "supports.video.capture", 
                                                       "supports.recording",
                                                       "audio.encodings", 
                                                       "video.encodings", 
                                                       "video.snapshot.encodings",
                                                       "streamable.contents" 
                                                     };
    
    private static final String[] PROPERTIES_BLUETOOTH = { "bluetooth.api.version",
                                                           "bluetooth.l2cap.receiveMTU.max",
                                                           "bluetooth.connected.devices.max",
                                                           "bluetooth.connected.inquiry", 
                                                           "bluetooth.connected.page",
                                                           "bluetooth.connected.inquiry.scan",
                                                           "bluetooth.connected.page.scan", 
                                                           "bluetooth.master.switch",
                                                           "bluetooth.sd.trans.max", 
                                                           "bluetooth.sd.attr.retrievable.max" 
                                                         };

    private static final String[] PROPERTIES_FILECONN = { "microedition.io.file.FileConnection.version",
                                                          "microedition.pim.version",
                                                          "fileconn.dir.photos", 
                                                          "fileconn.dir.videos",
                                                          "fileconn.dir.graphics", 
                                                          "fileconn.dir.tones",
                                                          "fileconn.dir.music", 
                                                          "fileconn.dir.recordings",
                                                          "fileconn.dir.memorycard", 
                                                          "fileconn.dir.private",
                                                          "fileconn.dir.photos.name", 
                                                          "fileconn.dir.videos.name",
                                                          "fileconn.dir.graphics.name", 
                                                          "fileconn.dir.tones.name",
                                                          "fileconn.dir.music.name",
                                                          "fileconn.dir.recordings.name",
                                                          "fileconn.dir.memorycard.name",
                                                          "fileconn.dir.private.name", 
                                                          "fileconn.dir.roots.names",
                                                          "file.separator" 
                                                         };

    private static final String[] PROPERTIES_WMA = { "wireless.messaging.version",
                                                     "wireless.messaging.sms.smsc", 
                                                     "wireless.messaging.mms.mmsc" 
                                                   };

    private static final String[] PROPERTIES_SATSA = { "microedition.satsa.crypto.version",
                                                       "microedition.satsa.apdu.version",
                                                       "microedition.satsa.pki.version", 
                                                       "microedition.smartcardslots" 
                                                     };

    private static final String[] PROPERTIES_WEB_SERVICES = { "xml.jaxp.subset.version", 
                                                              "xml.rpc.subset.version" 
                                                            };

    private static final String[] PROPERTIES_AMMS = { "microedition.amms.version",
                                                      "supports.mediacapabilities",
                                                      "tuner.modulations", 
                                                      "audio.samplerates",
                                                      "audio3d.simultaneouslocations", 
                                                      "camera.orientations",
                                                      "camera.resolutions" 
                                                    };

    private static final String[] PROPERTIES_M2G = { "microedition.m2g.version",
                                                     "microedition.m2g.svg.baseProfile", 
                                                     "microedition.m2g.svg.version" 
                                                   };

    private static final String[] PROPERTIES_M3G = { "microedition.m3g.version",
                                                     "supportAntialiasing", 
                                                     "supportTrueColor", 
                                                     "supportDithering",
                                                     "supportMipmapping", 
                                                     "supportPerspectiveCorrection",
                                                     "supportLocalCameraLighting", 
                                                     "maxLights", 
                                                     "maxViewportWidth",
                                                     "maxViewportHeight", 
                                                     "maxViewportDimension", 
                                                     "maxTextureDimension",
                                                     "m3gRelease", 
                                                     "maxSpriteCropDimension", 
                                                     "numTextureUnits",
                                                     "maxTransformsPerVertex" 
                                                   };

    private static final String[] PROPERTIES_CHAPI = { "microedition.chapi.version",
                                                     };

    private static final String[] PROPERTIES_NOKIA = { "com.nokia.mid.msisdn",
                                                       "com.nokia.mid.imei", 
                                                       "com.nokia.mid.imsi",
                                                       "com.nokia.mid.networkid", 
                                                       "com.nokia.mid.networksignal",
                                                       "com.nokia.mid.networkavailability",
                                                       "com.nokia.mid.networkstatus",
                                                       "com.nokia.mid.mnc",
                                                       "com.nokia.mid.cellid",
                                                       "com.nokia.mid.lac",
                                                       "com.nokia.mid.spn",
                                                       "com.nokia.mid.ons",
                                                       "com.nokia.mid.gid1",
                                                       "com.nokia.mid.gid2",
                                                       "com.nokia.mid.productcode",
                                                       "com.nokia.mid.batterylevel", 
                                                       "com.nokia.mid.countrycode",
                                                       "com.nokia.mid.dateformat", 
                                                       "com.nokia.mid.timeformat",
                                                       "com.nokia.network.access",
                                                       "com.nokia.memoryramfree",
                                                       "com.nokia.midp.impl.isa.network.databearer",
                                                       "Cell-ID",
                                                       "NAI",
                                                       "ESN",
                                                       "MEID",
                                                       "MIN",
                                                       "MDN",
                                                       "IMSI",
                                                       "device_id_imsi",
                                                       "User-Agent",
                                                       "Browser Header User Agent",
                                                       "DEVICE_MANUFACTURER",
                                                       "DEVICE_MODEL",
                                                       "SOFTWARE_VERSION"
                                                     };

    private static final String[] PROPERTIES_SE = { "com.sonyericsson.java.platform",
                                                    "com.sonyericsson.imei",
                                                    "com.sonyericsson.sim.subscribernumber",
                                                    "com.sonyericsson.net.isonhomeplmn",
                                                    "com.sonyericsson.net.status",
                                                    "com.sonyericsson.net.mcc",
                                                    "com.sonyericsson.net.mnc",
                                                    "com.sonyericsson.net.cmcc",
                                                    "com.sonyericsson.net.cmnc",
                                                    "com.sonyericsson.net.cellid",
                                                    "com.sonyericsson.net.lac",
                                                    "com.sonyericsson.net.rat",
                                                    "com.sonyericsson.net.networkname",
                                                    "com.sonyericsson.net.serviceprovider",
                                                    "com.sonyericsson.jackknifeopen",
                                                    "com.sonyericsson.flipopen",
                                                    "com.sonyericsson.active_profile",
                                                    "com.sonyericsson.active_alarm",
                                                    "camera.mountorientation"
                                                  };

    private static final String[] PROPERTIES_SAMSUNG = { "MCC",
                                                         "device.mcc",
                                                         "MNC",
                                                         "device.mnc",
                                                         "LocAreaCode",
                                                         "CellID",
                                                         "CELLID",
                                                         "LAC",
                                                         "phone.cid",
                                                         "NETWORK",
                                                         "device.network"
                                                       };

    private static final String[] PROPERTIES_MOTOROLA = { "device.software.version",
                                                          "device.model",
                                                          "language.direction",
                                                          "com.motorola.IMEI",
                                                          "IMEI",
                                                          "IMSI",
                                                          "MSISDN",
                                                          "CellID",
                                                          "LocAreaCode",
                                                          "com.mot.carrier.URL",
                                                          "batterylevel",
                                                          "funlight.product",
                                                          "midp_selector_exit",
                                                          "midp_alert_done",
                                                          "midp_command_menu",
                                                          "midp_selector_launch_failed",
                                                          "midp_vertical_scroll",
                                                          "midp_scrollbar_width",
                                                          "midp_screen_background",
                                                          "midp_screen_foreground",
                                                          "midp_screen_linked_color",
                                                          "midp_enhance_create_cropped_image",
                                                          "midp_enhance_flush_game_graphics",
                                                          "midp_game_flush_compatible_wt_wtk",
                                                          "location_category_max_length",
                                                          "location_landmark_max_length",
                                                          "MAType",
                                                          "GPRSState"
                                                        };

    private static final String[] PROPERTIES_IDEN = { "iden.device.model",
                                                      "iDEN-keyboard.attached", 
                                                      "iden.device.supports.mms",
                                                      "iden.device.supports.sms",
                                                      "iden.device.speech.recognition.dsr.capability",
                                                      "iden.device.baudrate",
                                                      "iden.available_language.en",
                                                      "iden.available_language.es",
                                                      "iden.available_language.pt",
                                                      "iden.available_language.fr",
                                                      "iden.available_language.ko",
                                                      "iden.available_language.he",
                                                      "PTN",
                                                      "IP",
                                                      "SIM",
                                                      "SERIAL"
                                                    };
    
    
    public static void evaluate(DiscoveryMIDlet midlet)
    {
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("SYSTEM PROPERTIES:");
        Logger.log("");

        //show available properties
        midlet.setTestStatus("Testing properties...");
        logProperties();
        checkProperties(PROPERTIES_CHECK);

        Logger.log("-----------------------------------");
    }

    
    private SystemProperties()
    {
        super();
    }

    private static void logProperties()
    {
        Logger.log("+ CLDC & MIDP:");
        logProperties(PROPERTIES_CLDC);

        Logger.log("");
        Logger.log("+ FileConnection API (JSR-75):");
        logProperties(PROPERTIES_FILECONN);

        Logger.log("");
        Logger.log("+ MMAPI (JSR-135):");
        logProperties(PROPERTIES_MMAPI);

        Logger.log("");
        Logger.log("+ WMA (JSR-120/205)");
        logProperties(PROPERTIES_WMA);

        Logger.log("");
        Logger.log("+ Content Handler API (JSR-211)");
        logProperties(PROPERTIES_CHAPI);

        Logger.log("");
        Logger.log("+ Bluetooth API (JSR-82):");
        logProperties(PROPERTIES_BLUETOOTH);

        Logger.log("");
        Logger.log("+ SATSA API (JSR-177):");
        logProperties(PROPERTIES_SATSA);

        Logger.log("");
        Logger.log("+ Web Services API (JSR-172):");
        logProperties(PROPERTIES_WEB_SERVICES);

        Logger.log("");
        Logger.log("+ AMMS API (JSR-234):");
        logProperties(PROPERTIES_AMMS);

        Logger.log("");
        Logger.log("+ 2D Vector Graphics API (JSR-226)");
        logProperties(PROPERTIES_M2G);

        Logger.log("");
        Logger.log("+ Mobile 3D Graphics API (JSR-184)");
        logProperties(PROPERTIES_M3G);

        Logger.log("");
        Logger.log("+ Nokia Properties:");
        logProperties(PROPERTIES_NOKIA);

        Logger.log("");
        Logger.log("+ SonyEricsson Properties:");
        logProperties(PROPERTIES_SE);

        Logger.log("");
        Logger.log("+ Samsung Properties:");
        logProperties(PROPERTIES_SAMSUNG);

        Logger.log("");
        Logger.log("+ Motorola Properties:");
        logProperties(PROPERTIES_MOTOROLA);

        Logger.log("");
        Logger.log("+ iDEN Properties:");
        logProperties(PROPERTIES_IDEN);
    }
    
    private static void logProperties(String[] properties)
    {
        int propertyCount = 0;
        for (int i = 0; i < properties.length; i++)
        {
            String propertyValue = logProperty(properties[i]);
            if (propertyValue != null)
                propertyCount++;
        }
        
        if (propertyCount <= 0)
            Logger.log("    None.");
    }

    private static String logProperty(String property)
    {
        try
        {
            String propertyValue = System.getProperty(property);
            if (propertyValue != null)
            {
                Logger.log("    " + property + ":");
                Logger.log("        [" + propertyValue + "]");
            }
            
            return propertyValue;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_LOW, "Property ('" + property + "'): failed to retrieve property value", e);
            return null;
        }
    }

    private static void checkProperties(String[] properties)
    {
        for (int i = 0; i < properties.length; i++)
        {
            String propertyValue = System.getProperty( properties[i] );
            if (propertyValue == null)
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "Property ('" + properties[i] + "'): property is not supported");
        }
    }
}
