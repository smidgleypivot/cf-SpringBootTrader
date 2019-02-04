package io.pivotal.pa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

public class NewRelicEventsGenerator {

    //NEW RELIC INSIGHTS URL to insert
    //final static String insightsAccountID = "2148181";
    //final static String insertInsightsKey = "jWn33zgslQlNTuVCQZrB_29a9jNCbd0s";
    final static String NRimportInsights = "https://insights-collector.eu01.nr-data.net/v1/accounts/%s/events";
    final static String eventTypeService = "ServiceTickets";
    final static String eventTypeApp = "AppDeploymentEvent";
    final static Date today = new Date();
    //Date dateCreated = new Date();
    static String[] testTypes = new String[]{"New Server", "System Down", "New Feature", "Bug Fix", "Bug Fix", "Bug Fix", "New Feature"};
    static String[] testStatuses = new String[]{"Closed", "Open", "Reopened", "Closed"};
    static String[] testApps = new String[]{"webtrader", "users", "quotes", "portfolio", "accounts"};
    static String[] testDescriptions = new String[]{"Added code", "Edited code", "Removed faulty code"};
    static String[] testOwners = new String[]{"braj@newrelic.com", "shahram@newrelic.com", "adasgupta@newrelic.com", "tpasin@newrelic.com", "rramanujam@newrelic.com"};


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong number of args - expected 2 - the insights account id and the insights apm key ");
            System.exit(-1);
        }

        String apmId = args[0];
        String apiToken = args[1];
        final JSONArray events = new JSONArray();
        int numberOfNewTickets = generateRandInt(7);
        int numberOfLegacyTickets = numberOfNewTickets * 2;
        IntStream.range(0,numberOfNewTickets).forEach(i -> {
            List<JSONObject> serviceTickets = generateServiceTickets(false);
            serviceTickets.forEach(st -> events.put(st));
        });
        IntStream.range(0,numberOfLegacyTickets).forEach(i -> {
            List<JSONObject> serviceTickets = generateServiceTickets(true);
            serviceTickets.forEach(st -> events.put(st));
        });
        postToInsights(events, apmId, apiToken);

        //create events for page views
        int numberOfUsers = generateRandInt(150);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        IntStream.range(0, numberOfUsers).forEach(i -> {
            String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
            PageViews pvs = new PageViews(latch, sessionId, apmId, apiToken);
            Thread newThread = new Thread(pvs);
            newThread.start();
        });
        try {
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private static List<JSONObject> generateServiceTickets(boolean legacy) {
        List<JSONObject> serviceTickets = new ArrayList<>();
        int severity = generateRandInt(10) + 1;
        if (severity > 1 && severity < 4) {
            severity = 2;
        } else if (severity > 3 && severity < 6) {
            severity = 3;
        } else if (severity > 5 && severity < 11) {
            severity = 4;
        } else if (severity == 1 && !legacy) {
            //we dont raise any sev 1 tickets against our new apps running on PCF with new relic ;)
            severity = 2;
        }
        String owner = testOwners[generateRandInt(testOwners.length - 1)];
        String type = testTypes[generateRandInt(testTypes.length - 1)];
        int timeToResolution = generateRandInt(7) + 1;
        if (legacy) {
            timeToResolution *= 2;
        } else {
            timeToResolution /= 5;
        }
        String status = testStatuses[generateRandInt(testStatuses.length - 1)];
        String app = testApps[generateRandInt(testApps.length - 1)];
        if (legacy) {
            app = app + "_legacy";
        }
        int ticket = generateRandInt(99999 - 10000) + 10000;

        try {

            JSONObject toAdd = new JSONObject();
            toAdd.put("eventType", eventTypeService);
            toAdd.put("type", type);
            toAdd.put("severity", severity);
            toAdd.put("duration", timeToResolution);
            toAdd.put("legacy", legacy);
            toAdd.put("creationDate", today);
            toAdd.put("owner", owner);
            toAdd.put("status", status);
            toAdd.put("appName", app);
            toAdd.put("ticketNum", ticket);

            serviceTickets.add(toAdd);
            //if it is a bug, our fast paced agile teams running on new relic and PCF can fix and deploy a patch in the same day
            if (type.equalsIgnoreCase("Bug Fix") || type.equalsIgnoreCase("New Feature")) {
                serviceTickets.add(createDeploymentEvent(type, ticket, app, legacy));
            }
        } catch (Exception e) {

        }
        return serviceTickets;
    }



    private static JSONObject createDeploymentEvent(String type, int ticket, String app, boolean legacy) {

        String revision = "1." + ((Integer) generateRandInt(10)).toString() + "." + ((Integer) generateRandInt(10)).toString();
        JSONObject toAdd = new JSONObject();

        int severity = generateRandInt(3) + 1;
        String owner = testOwners[generateRandInt(testOwners.length - 1)];
        int developmentTime = generateRandInt(5) + 1;
        if (legacy) {
            developmentTime *= 5;
        }
        int codeCommits = generateRandInt(4) + 1;

        try {
            toAdd.put("eventType", eventTypeApp);
            toAdd.put("appName", app);
            toAdd.put("type", type);
            toAdd.put("revision", revision);
            toAdd.put("severity", severity);
            toAdd.put("description", testDescriptions[generateRandInt(testDescriptions.length - 1)]);
            toAdd.put("creationDate", today);
            toAdd.put("legacy", legacy);
            toAdd.put("owner", owner);
            toAdd.put("developmentTime", developmentTime);
            toAdd.put("codeCommits", codeCommits);
            toAdd.put("ticketNum", ticket);

            //System.out.println(importArr);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        return toAdd;
    }

    static int generateRandInt(int bound) {

        Random rand = new Random();
        return rand.nextInt(bound);
    }

    static void postToInsights(JSONArray importArr, String apmId, String accessToken) {
        System.out.println("Posting Data to new relic");
        System.out.println(importArr.toString());
        String NRjson = "";
        try {

            URL url = new URL(String.format(NRimportInsights, apmId));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            //conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-Insert-Key", accessToken);

            OutputStream os = conn.getOutputStream();
            os.write(importArr.toString().getBytes("UTF-8"));
            os.flush();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output = "";

            while ((output = br.readLine()) != null) {
                NRjson = NRjson + output;
                //System.out.println(output + "\n");
            }
            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }
}

class PageViews implements Runnable {
    final static String WEB_APP_NAME = "webtrader";
    final static String ORGANISATION = "NA";
    final static Integer APP_ID = 1699345;
    final static String ASN = "8220";
    final static Double LATITUDE = 51.51;
    final static Double LONGITUDE = -0.09;
    final static String BROWSER_TRANSACTION_NAME_TEMPLATE = "webtrader-prod.cfapps.lab01.pcf.pw:443/%s";
    final static String CITY = "London";
    final static String COUNTRY = "GB";
    final static String DEVICE = "Desktop";
    final static String DOMAIN = "webtrader-prod.cfapps.lab01.pcf.pw";
    final static String NAME_PREFIX_TEMPLATE = "WebTransaction/SpringController/%s (GET)";
    final static String[] PATHS = {"home", "portfolio", "accounts", "trade"};
    final static String PAGE_URL_TEMPLATE = "https://webtrader-prod.cfapps.lab01.pcf.pw/%s";
    final static String USER_AGENT = "Chrome";
    final static String USER_AGENT_OS =  "Mac";
    final static String USER_AGENT_VERSION = "71";


    private CountDownLatch latch;
    private String sessionId;
    private String apmId;
    private String apiToken;

    public PageViews(CountDownLatch latch, String sessionId, String apmId, String apiToken) {
        this.latch = latch;
        this.sessionId = sessionId;
        this.apmId = apmId;
        this.apiToken = apiToken;
    }

    @Override
    public void run() {
        int numberOfViewsInSession = NewRelicEventsGenerator.generateRandInt(150);
        IntStream.range(0, numberOfViewsInSession).forEach(i -> {
            final JSONArray events = new JSONArray();
            String path = PATHS[NewRelicEventsGenerator.generateRandInt(PATHS.length)];
            JSONObject pageViewEvent = new JSONObject();
            pageViewEvent.put("eventType", "PageView");
            pageViewEvent.put("timestamp", new Date());
            pageViewEvent.put("appId", APP_ID);
            pageViewEvent.put("appName", WEB_APP_NAME);
            pageViewEvent.put("asn", ASN);
            pageViewEvent.put("asnLatitude", LATITUDE);
            pageViewEvent.put("asnLongitude", LONGITUDE);
            pageViewEvent.put("asnOrganisation", ORGANISATION);
            pageViewEvent.put("backendDuration", Math.random() * 2 );
            pageViewEvent.put("browserTransactionName", String.format(BROWSER_TRANSACTION_NAME_TEMPLATE, path));
            pageViewEvent.put("city", CITY);
            pageViewEvent.put("connectionSetupDuration", 0);
            pageViewEvent.put("countryCode", COUNTRY);
            pageViewEvent.put("deviceType", DEVICE);
            pageViewEvent.put("dnsLookupDuration", 0);
            pageViewEvent.put("domProcessingDuration", Math.random() / 20);
            pageViewEvent.put("domain", DOMAIN);
            pageViewEvent.put("duration", Math.random() * 2);
            pageViewEvent.put("name", String.format(NAME_PREFIX_TEMPLATE, path));
            pageViewEvent.put("networkDuration", Math.random() / 40);
            pageViewEvent.put("pageRenderingDuration", Math.random() / 50);
            pageViewEvent.put("pageUrl", String.format(PAGE_URL_TEMPLATE, path));
            pageViewEvent.put("queueDuration", 0.0);
            pageViewEvent.put("regionCode", "H9");
            pageViewEvent.put("secureHandshakeDuration", 0);
            pageViewEvent.put("session", sessionId);
            pageViewEvent.put("userAgentName", USER_AGENT);
            pageViewEvent.put("userAgentOS", USER_AGENT_OS);
            pageViewEvent.put("webAppDuration", Math.random() * 2);
            events.put(pageViewEvent);
            NewRelicEventsGenerator.postToInsights(events, apmId,apiToken );
            try {
                Thread.sleep(NewRelicEventsGenerator.generateRandInt(10000));
            } catch (Exception e ) {}
        });
        this.latch.countDown();
    }
}
