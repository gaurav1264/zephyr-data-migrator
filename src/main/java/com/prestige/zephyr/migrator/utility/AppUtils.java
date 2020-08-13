package com.prestige.zephyr.migrator.utility;

public class AppUtils {
    public static String getEndPoint(String baseURL,String endpointURL){
        return baseURL.replaceAll("/$","")+endpointURL;
    }
}
