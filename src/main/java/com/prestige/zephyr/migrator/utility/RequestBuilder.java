package com.prestige.zephyr.migrator.utility;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class RequestBuilder<T> {
    private HttpEntity<T> requests;

    public HttpEntity<T> withJsonHeadersandBody(T Body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Accept", "*/*");
        requests = new HttpEntity<T>(Body, headers);
        return requests;
    }

    public HttpEntity<T> withAuthHeaderJson(String username, String password) {
        String base64Cred = encodeToBase64(username + ":" + password);
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization","Basic " + base64Cred);
        headers.add("Content-Type","application/json");
        headers.add("Accept","application/json");
        requests = new HttpEntity<T>(headers);
        return requests;
    }
    public HttpEntity<T> withAuthHeaderandBody(String username, String password,T Body) {
        String base64Cred = encodeToBase64(username + ":" + password);
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization","Basic " + base64Cred);
        headers.add("Content-Type","application/json");
        headers.add("Accept","application/json");
        requests = new HttpEntity<T>(Body,headers);
        return requests;
    }
    public HttpEntity<T> withAuthHeaderXml(String username, String password) {
        String base64Cred = encodeToBase64(username + ":" + password);
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.APPLICATION_XML);
        headers.add("Authorization", "Basic " + base64Cred);
        headers.add("Content-Type","application/xml");
        headers.add("Accept","*/*");
        requests = new HttpEntity<T>(headers);
        return requests;
    }
    public HttpEntity<T> withAuthHeader(String username, String password) {
        String base64Cred = encodeToBase64(username + ":" + password);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Cred);
        headers.add("Content-Type","application/xml");
        headers.add("Accept","*/*");
        requests = new HttpEntity<T>(headers);
        return requests;
    }

    private String encodeToBase64(String plainCred) {
        byte[] plainCredentials = plainCred.getBytes();
        byte[] base64Cred = Base64.encodeBase64(plainCredentials);
        String resturnStr = new String(base64Cred);
        return resturnStr;

    }
}
