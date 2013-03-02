package org.postronic.h3.upnp;


public class UPnPUserAgent {
    
    private final String osName, osVersion, productName, productVersion;
    
    public UPnPUserAgent(String productName, String productVersion) {
        this(productName, productVersion, System.getProperty("os.name"), System.getProperty("os.version"));
    }
    
    public UPnPUserAgent(String productName, String productVersion, String osName, String osVersion) {
        this.osName = osName;
        this.osVersion = osVersion;
        this.productName = productName;
        this.productVersion = productVersion;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public String getProductVersion() {
        return productVersion;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(osName.replace(" ", "-")).append("/").append(osVersion.replace(" ", "-"));
        sb.append(" UPnP/1.1 ");
        sb.append(productName.replace(" ", "-")).append("/").append(productVersion.replace(" ", "-"));
        return sb.toString();
    }
    
}
