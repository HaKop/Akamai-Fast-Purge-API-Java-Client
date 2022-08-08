package com.hakop.akamai;

import com.akamai.edgegrid.signer.ClientCredential;
import com.akamai.edgegrid.signer.apachehttpclient.ApacheHttpClientEdgeGridInterceptor;
import com.akamai.edgegrid.signer.apachehttpclient.ApacheHttpClientEdgeGridRoutePlanner;
import com.hakop.akamai.purgetask.PurgeTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;


/**
 * CLI Tool to invalidate content stored on Akamai CDN.
 * <p>
 * If URLs and/or Cache-Tags need to be purged it will make use of the Akamai CCU V3 (Fast Purge) API
 * which invalidates content almost immediately:
 * <a href="https://developer.akamai.com/api/core_features/fast_purge/v3.html" />
 * <p>
 * If directories (sub-paths) need to be purged it will make use of the Akamai ECCU V1 API
 * which invalidates content with a delay of roughly 30 minutes:
 * <a href="https://developer.akamai.com/api/core_features/enhanced_content_control_utility/v1.html" />
 * <p>
 * Provide the Akamai config-file for this tool:
 * <p>
 * -c, --config-file   path to the Akamai config file containing i.a. access tokens
 * <p>
 * Optional parameters:
 * <p>
 * -u, --urls           comma-separated list of URLs to be invalidated on Akamai
 * -t, --tags           comma-separated list of cache-tags to be invalidated on Akamai
 * -r, --recursive      also invalidate all sub-paths of provided URL list
 * -m, --mail           e-mail address for notification (only for recursive purges)
 */
@Slf4j
public class AkamaiPurgeClient {
    private static final Options OPTIONS = new Options();

    private final Set<PurgeTask> purgeJobs = new LinkedHashSet<>();

    static {
        OPTIONS.addRequiredOption("c", "config-file", true, "Path to config file");
        OPTIONS.addOption("u", "urls", true, "List of comma-separated URLs to purge");
        OPTIONS.addOption("t", "tags", true, "List of comma-separated cache-tags to purge");
        OPTIONS.addOption("cpcodes", true, "List of comma-separated CP codes to purge");
        OPTIONS.addOption("r", "recursive", false, "Invalidate sub-paths recursively");
        OPTIONS.addOption("m", "mail", true, "E-Mail address for notification");
    }

    public static void main(String[] args) {
        AkamaiPurgeClient akamaiPurgeClient = new AkamaiPurgeClient();
        akamaiPurgeClient.parseArgs(args);
        akamaiPurgeClient.purge();
    }

    private void purge() {
        purgeJobs.forEach(purgeJob -> {
            URI apiEndpoint = purgeJob.getEndpointUrl();
            ClientCredential clientCredentials = purgeJob.getCredentials();
            String payload = purgeJob.getPayload();
            HttpPost request = new HttpPost(apiEndpoint);
            log.info("Endpoint: {}; Items to purge: ({})", apiEndpoint, payload);
            request.setEntity(new StringEntity(payload, ContentType.create("application/json")));

            int httpStatus = -1;
            String responseString;

            try (CloseableHttpClient httpClient = getClient(clientCredentials)){
                HttpResponse response = httpClient.execute(request);
                httpStatus = response.getStatusLine().getStatusCode();
                responseString = new BasicResponseHandler().handleResponse(response);
                log.info("HTTP Status: {}", httpStatus);
                log.info("Akamai Purge Response: {}", responseString);
            } catch (IOException ioe) {
                log.error("Something went wrong trying to purge Akamai cache: ", ioe);
            }

            if (httpStatus != 201) {
                System.exit(1);
            }
        });
    }

    private void parseArgs(String... args) {
        CommandLine cmd;

        try {
            cmd = new DefaultParser().parse(OPTIONS, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse provided arguments.", e);
        }

        File configFile = new File(cmd.getOptionValue("config-file"));
        Properties configProps = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            configProps.load(fis);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to process provided config file.", e);
        }

        if (cmd.hasOption("mail")) {
            configProps.putAll(cmd.getOptionProperties("mail"));
        }

        String[] purgeItems;
        if (cmd.hasOption("urls")) {
            purgeItems = cmd.getOptionValue("urls").split(",");
            if (cmd.hasOption("recursive")) {
                purgeJobs.add(PurgeTask.of(PurgeTask.PurgeTaskType.URLS_RECURSIVE, configProps, purgeItems));
            } else {
                purgeJobs.add(PurgeTask.of(PurgeTask.PurgeTaskType.URLS, configProps, purgeItems));
            }
        }
        if (cmd.hasOption("tags")) {
            purgeItems = cmd.getOptionValue("tags").split(",");
            purgeJobs.add(PurgeTask.of(PurgeTask.PurgeTaskType.CACHE_TAGS, configProps, purgeItems));
        }
        if (cmd.hasOption("cpcodes")) {
            purgeItems = cmd.getOptionValue("cpcodes").split(",");
            purgeJobs.add(PurgeTask.of(PurgeTask.PurgeTaskType.CP_CODES, configProps, purgeItems));
        }
    }

    private CloseableHttpClient getClient(ClientCredential credentials) {
        return HttpClientBuilder.create()
                .addInterceptorFirst(new ApacheHttpClientEdgeGridInterceptor(credentials))
                .setRoutePlanner(new ApacheHttpClientEdgeGridRoutePlanner(credentials))
                .build();
    }
}