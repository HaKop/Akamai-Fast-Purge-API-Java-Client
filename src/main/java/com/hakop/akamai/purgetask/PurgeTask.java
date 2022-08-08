package com.hakop.akamai.purgetask;

import com.akamai.edgegrid.signer.ClientCredential;

import java.net.URI;
import java.util.Properties;

public interface PurgeTask {
    enum PurgeTaskType {
        URLS,
        URLS_RECURSIVE,
        CACHE_TAGS,
        CP_CODES
    }

    static PurgeTask of(PurgeTaskType type, Properties configProps, String[] purgeItems) {
        switch (type) {
            case URLS:
                return new UrlPurgeTask(configProps, purgeItems);
            case URLS_RECURSIVE:
                return new RecursiveUrlPurgeTask(configProps, purgeItems);
            case CACHE_TAGS:
                return new CacheTagPurgeTask(configProps, purgeItems);
            case CP_CODES:
                return new CPCodePurgeTask(configProps, purgeItems);
            default:
                return null;
        }
    }

    ClientCredential getCredentials();
    URI getEndpointUrl();
    String getPayload();
}
