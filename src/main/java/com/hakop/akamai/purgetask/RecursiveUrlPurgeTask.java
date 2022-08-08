package com.hakop.akamai.purgetask;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class RecursiveUrlPurgeTask extends UrlPurgeTask {
    public static final String AKAMAI_ECCU_DIRECTORIES_API_URL = "https://%s/eccu-api/v1/requests";
    // eccu api config keys
    protected static final String PROP_KEY_ECCU_HOST = "eccu_host";
    protected static final String PROP_KEY_ECCU_CLIENT_SECRET = "eccu_client_secret";
    protected static final String PROP_KEY_ECCU_CLIENT_TOKEN = "eccu_client_token";
    protected static final String PROP_KEY_ECCU_ACCESS_TOKEN = "eccu_access_token";

    public RecursiveUrlPurgeTask(Properties configProps, String[] purgeItems) {
        super(configProps, purgeItems);
    }

    @Override
    public String getPayload() {
        Map<String, Object> map = new HashMap<>();
        String metadata = createXML(getPurgeItemList());
        map.put("metadata", metadata);
        map.put("propertyName", getPurgeItemList().get(0).getHost());
        map.put("propertyNameExactMatch", true);
        map.put("propertyType", "HOST_HEADER");
        String mail = getConfigProps().getProperty("mail");
        if (StringUtils.isNotBlank(mail)) {
            map.put("statusUpdateEmails", Collections.singletonList(mail));
        }
        return GSON.toJson(map);
    }

    @Override
    protected List<URL> validateAndBuildPurgeItems(String[] purgeItems) {
        List<URL> urlList = super.validateAndBuildPurgeItems(purgeItems);

        while (!urlList.isEmpty()) {
            String currentHost = urlList.get(0).getHost();
            List<URL> currentURLs = urlList.stream()
                    .filter(url -> StringUtils.equals(url.getHost(), currentHost))
                    .collect(Collectors.toList());
            urlList.removeAll(currentURLs);
        }

        return urlList;
    }

    @Override
    protected String getHostTemplate() {
        return AKAMAI_ECCU_DIRECTORIES_API_URL;
    }

    @Override
    protected EdgeConfig readConfig(Properties configProps) {
        String host = configProps.getProperty(PROP_KEY_ECCU_HOST);
        String clientSecret = configProps.getProperty(PROP_KEY_ECCU_CLIENT_SECRET);
        String clientToken = configProps.getProperty(PROP_KEY_ECCU_CLIENT_TOKEN);
        String accessToken = configProps.getProperty(PROP_KEY_ECCU_ACCESS_TOKEN);

        return new EdgeConfig(host, clientSecret, clientToken, accessToken);
    }

    private String createXML(List<URL> urls) {
        String xml = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("eccu");
            doc.appendChild(root);

            Map<String, Map> pathMap = new HashMap<>();
            urls.forEach(url -> addToMap(pathMap, url.getPath().split("/")));
            mapToXml(pathMap, root);

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();

            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            xml = writer.getBuffer().toString();
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
        return xml;
    }

    private void addToMap(Map<String, Map> bigMap, String[] pathParts) {
        boolean isPresent = false;
        for (String pathPart : pathParts) {
            if (StringUtils.isBlank(pathPart)) {
                continue;
            } else if (bigMap.containsKey(pathPart)) {
                isPresent = true;
            } else  if (isPresent && bigMap.isEmpty()){
                return;
            } else {
                bigMap.put(pathPart, new HashMap<>());
            }
            bigMap = bigMap.get(pathPart);
        }
        bigMap.clear();
    }

    private void mapToXml(Map<String, Map> map, Element element) {
        if (map.isEmpty()) {
            Element el = element.getOwnerDocument().createElement("revalidate");
            el.setTextContent("now");
            element.appendChild(el);
        }
        map.keySet().forEach(key -> {
            Element el = element.getOwnerDocument().createElement("match:recursive-dirs");
            el.setAttribute("value", key);
            mapToXml(map.get(key), el);
            element.appendChild(el);
        });
    }
}
