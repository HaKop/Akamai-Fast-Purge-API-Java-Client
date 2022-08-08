package com.hakop.akamai.purgetask;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class CPCodePurgeTask extends AbstractPurgeTask<Integer> {
    private static final String AKAMAI_FASTPURGE_CPCODES_API_URL = "https://%s/ccu/v3/invalidate/cpcode/production";

    protected CPCodePurgeTask(Properties configProps, String[] purgeItems) {
        super(configProps, purgeItems);
    }

    @Override
    protected List<Integer> validateAndBuildPurgeItems(String[] purgeItems) {
        return Arrays.stream(purgeItems)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    @Override
    protected String getHostTemplate() {
        return AKAMAI_FASTPURGE_CPCODES_API_URL;
    }
}
