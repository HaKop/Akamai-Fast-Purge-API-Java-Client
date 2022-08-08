package com.hakop.akamai.purgetask;

import java.util.List;
import java.util.Properties;

public class CacheTagPurgeTask extends AbstractPurgeTask<String> {
    private static final String AKAMAI_FASTPURGE_TAGS_API_URL = "https://%s/ccu/v3/invalidate/tag/production";

    protected CacheTagPurgeTask(Properties configProps, String[] purgeItems) {
        super(configProps, purgeItems);
    }

    @Override
    protected List<String> validateAndBuildPurgeItems(String[] purgeItems) {
        return List.of(purgeItems);
    }

    @Override
    protected String getHostTemplate() {
        return AKAMAI_FASTPURGE_TAGS_API_URL;
    }
}
