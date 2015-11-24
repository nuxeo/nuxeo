package com.okta.saml.util;

import com.okta.saml.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

public class IPRange {
    private String[] startAddress;
    private String[] endAddress;

    private static final Logger logger = LoggerFactory.getLogger(IPRange.class);

    public IPRange(String startAddress, String endAddress) {
        validateIp(startAddress);
        this.startAddress = parseAddressString(startAddress);

        if (endAddress != null && !endAddress.trim().isEmpty()) {
            validateIp(endAddress);
            this.endAddress = parseAddressString(endAddress);
        }
    }

    public void validateIp(String ip) {
        boolean valid = true;
        String[] parts = parseAddressString(ip);
        if (parts == null) {
            throw new NumberFormatException(String.format("Failed to parse address %s", ip));
        }
        for (String item : parts) {
            if (StringUtils.isNumeric(item)) {
                int val = Integer.parseInt(item);
                if (val < 0 || val > 255) {
                    valid = false;
                }
            } else if (!StringUtils.equals(item, "*")) {
                valid = false;
            }
        }
        if (!valid) {
            throw new NumberFormatException(String.format("All IP address segments should be from 0 to 255 or *, %s provided", ip));
        }
    }

    public String[] parseAddressString(String address) {
        String[] retVal = new String[4];
        if (IPAddressUtil.isIPv6LiteralAddress(address)) {
            logger.error(String.format("IPv6 address provided: %s, IP range mechanism will not be available.", address));
            return null;
        }
        
        String[] parts = address.split("\\.");
        int i = 0;
        for (String part : parts) {
            retVal[i++] = part;
        }
        
        return retVal;
    }

    public boolean isAddressInRange(String address) {
        String[] startAddress = getStartAddress();
        String[] endAddress = getEndAddress();
        String[] testAddress = parseAddressString(address);

        validateIp(address);

        if (null == endAddress) {
            for (int iPos = 0; iPos < 4; iPos++) {
                if (!StringUtils.equals(startAddress[iPos], "*") && !StringUtils.equals(startAddress[iPos], testAddress[iPos])) {
                    return false;
                }
            }
        } else {
            for (int iPos = 0; iPos < 4; iPos++) {
                int startValue = StringUtils.equals(startAddress[iPos], "*") ? 0 : Integer.parseInt(startAddress[iPos]);
                int endValue = StringUtils.equals(endAddress[iPos], "*") ? 255 : Integer.parseInt(endAddress[iPos]);
                int testAddressValue = Integer.parseInt(testAddress[iPos]);
                if (!(testAddressValue >= startValue && testAddressValue <= endValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String[] getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String[] startAddress) {
        this.startAddress = startAddress;
    }

    public String[] getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String[] endAddress) {
        this.endAddress = endAddress;
    }
}