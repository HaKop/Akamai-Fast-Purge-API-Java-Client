package com.hakop.akamai.purgetask;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class UrlPurgeTask extends AbstractPurgeTask<URL> {
    private static final String AKAMAI_FASTPURGE_URLS_API_URL = "https://%s/ccu/v3/invalidate/url/production";

    public UrlPurgeTask(Properties configProps, String[] purgeItems) {
        super(configProps, purgeItems);
    }

    @Override
    protected List<URL> validateAndBuildPurgeItems(String[] purgeItems) {
        return Arrays.stream(purgeItems)
                .map(spec -> {
                    try {
                        return new URL(spec);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Provided URL(s) malformed.", e);
                    }})
                .collect(Collectors.toList());
    }

    @Override
    protected String getHostTemplate() {
        return AKAMAI_FASTPURGE_URLS_API_URL;
    }
}
