/*
 * Copyright (C) 2016-2022 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.scaniatv;

/***********************************/
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
/***********************************/

import com.gmt2001.HttpRequest;
import com.gmt2001.httpclient.HttpClient;
import com.gmt2001.httpclient.HttpClientResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

/*
 * @author ScaniaTV
 */
public class StreamElementsAPIv2 {

    private static StreamElementsAPIv2 instance;
    private static final String URL = "https://api.streamelements.com/kappa/v2";
    private static String jwtToken = "";
    private String id = "";
    private int pullLimit = 5;

    /*
     * Returns the current instance.
     */
    public static synchronized StreamElementsAPIv2 instance() {
        if (instance == null) {
            instance = new StreamElementsAPIv2();
        }

        return instance;
    }

    /*
     * Builds the instance for this class.
     */
    private StreamElementsAPIv2() {
        Thread.setDefaultUncaughtExceptionHandler(com.gmt2001.UncaughtExceptionHandler.instance());
    }

    private static String sendJsonToUrl(String urlAddress, String requestJSON) throws JSONException {
        HttpsURLConnection connection = null;
        InputStream is = null;

        try {
            URL url = new URL(urlAddress);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Authorization", "Bearer " + jwtToken);
            connection.addRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.52 Safari/537.36 PhantomBotJ/2015");
            //connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            System.out.println(requestJSON);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(requestJSON);
            wr.close();

            //Get Response

            try {
                is = connection.getInputStream();
            } catch (IOException ioe) {
                int statusCode = connection.getResponseCode();
                if (statusCode != 200) {
                    is = connection.getErrorStream();
                }
            }

            assert is != null;
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));


            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {
            com.gmt2001.Console.err.printStackTrace(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    /*
     * Reads data from an API. In this case its tipeeestream.
     */
    @SuppressWarnings("UseSpecificCatch")
    private static JSONObject readJsonFromUrl(String endpoint) throws URISyntaxException {
        JSONObject jsonResult = new JSONObject("{}");
        HttpHeaders headers = HttpClient.createHeaders(HttpMethod.GET, true);
        headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + jwtToken);
        HttpClientResponse response = HttpClient.get(URI.create(URL + endpoint), headers);

        if (response.hasJson()) {
            jsonResult = response.json();
            HttpRequest.generateJSONObject(jsonResult, true, "GET", "", endpoint, response.responseCode().code(), null, null);
        } else {
            jsonResult.put("error", response.responseBody());
            HttpRequest.generateJSONObject(jsonResult, true, "GET", "", endpoint, response.responseCode().code(), null, null);
        }

        return jsonResult;
    }

    /*
     * Sets the jwt token to access the api
     *
     * @param  jwtToken  jwt key that the user added in the bot login.
     */
    public void SetJWT(String token) {
        jwtToken = token.trim();
    }

    /*
     * Sets the streamelements user account id
     *
     * @param  id
     */
    public void SetID(String id) {
        this.id = id.trim();
    }

    /*
     * Sets the api pull limit.
     *
     * @param  pullLimit  Amount of donations to pull, default is 5.
     */
    public void SetLimit(int pullLimit) {
        this.pullLimit = pullLimit;
    }

    /*
     * Pulls the 5 last donations from the API.
     *
     * @return  The last 5 donations from the api.
     */
    public JSONObject GetDonations() throws URISyntaxException {
        return readJsonFromUrl("/tips/" + this.id + "?limit=" + this.pullLimit);
    }

    public String AddTicketsToUsers(String[] users, int amount) {
        JSONObject dataToSend = new JSONObject();
        dataToSend.put("mode", "add");
        JSONArray userArray = new JSONArray();
        for (String user : users) {
            JSONObject userObj = new JSONObject();
            userObj.put("username", user);
            userObj.put("current", amount);
            userArray.put(userObj);
        }
        dataToSend.put("users", userArray);

        return sendJsonToUrl(URL + "/points/" + this.id, dataToSend.toString(0));
    }
}