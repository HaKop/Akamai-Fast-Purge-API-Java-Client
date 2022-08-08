package com.hakop.akamai.purgetask;

import com.akamai.edgegrid.signer.ClientCredential;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

abstract class AbstractPurgeTask<T> implements PurgeTask {
    protected static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    // fast purge api config keys
    private static final String PROP_KEY_FASTPURGE_HOST = "fastpurge_host";
    private static final String PROP_KEY_FASTPURGE_CLIENT_SECRET = "fastpurge_client_secret";
    private static final String PROP_KEY_FASTPURGE_CLIENT_TOKEN = "fastpurge_client_token";
    private static final String PROP_KEY_FASTPURGE_ACCESS_TOKEN = "fastpurge_access_token";

    @Getter(AccessLevel.PUBLIC)
    private final ClientCredential credentials;
    @Getter(AccessLevel.PUBLIC)
    private final URI endpointUrl;
    @Getter(AccessLevel.PROTECTED)
    private final List<T> purgeItemList = new ArrayList<>();
    @Getter(AccessLevel.PROTECTED)
    private final Properties configProps;

    protected AbstractPurgeTask(Properties configProps, String[] purgeItems) {
        this.configProps =configProps;
        this.credentials = validateAndBuildCredentials(configProps);
        this.endpointUrl = validateAndBuildEndpointUrl();
        this.purgeItemList.addAll(validateAndBuildPurgeItems(purgeItems));
    }

    @Override
    public String getPayload() {
        Map<String, Object> map = new HashMap<>();
        map.put("objects", purgeItemList);
        return GSON.toJson(map);
    }

    protected abstract List<T> validateAndBuildPurgeItems(String[] purgeItems);

    protected abstract String getHostTemplate();

    protected URI validateAndBuildEndpointUrl() {
        try {
            return new URI(String.format(getHostTemplate(), getCredentials().getHost()));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Provided host address is invalid.", e);
        }
    }

    protected ClientCredential validateAndBuildCredentials(Properties configProps) {
        EdgeConfig edgeConfig = readConfig(configProps);

        if (StringUtils.isAnyBlank(edgeConfig.host, edgeConfig.clientSecret, edgeConfig.clientToken, edgeConfig.accessToken)) {
            throw new IllegalArgumentException("Provided Akamai config is incomplete.");
        }

        return ClientCredential.builder()
                .accessToken(edgeConfig.accessToken)
                .clientToken(edgeConfig.clientToken)
                .clientSecret(edgeConfig.clientSecret)
                .host(edgeConfig.host)
                .build();
    }

    protected EdgeConfig readConfig(Properties configProps) {
        String host = configProps.getProperty(PROP_KEY_FASTPURGE_HOST);
        String clientSecret = configProps.getProperty(PROP_KEY_FASTPURGE_CLIENT_SECRET);
        String clientToken = configProps.getProperty(PROP_KEY_FASTPURGE_CLIENT_TOKEN);
        String accessToken = configProps.getProperty(PROP_KEY_FASTPURGE_ACCESS_TOKEN);

        return new EdgeConfig(host, clientSecret, clientToken, accessToken);
    }

    protected static class EdgeConfig {
        protected final String host;
        protected final String clientSecret;
        protected final String clientToken;
        protected final String accessToken;

        public EdgeConfig(String host, String clientSecret, String clientToken, String accessToken) {
            this.host = host;
            this.clientSecret = clientSecret;
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }
    }
}
