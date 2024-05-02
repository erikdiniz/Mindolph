package com.mindolph.base.genai.llm;

import com.mindolph.base.constant.PrefConstants;
import com.mindolph.core.constant.GenAiConstants;
import com.mindolph.mfx.preference.FxPreferences;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.mindolph.base.constant.PrefConstants.GEN_AI_TIMEOUT;

/**
 * @author mindolph.com@gmail.com
 * @since 1.7
 */
public abstract class BaseLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(BaseLlmProvider.class);

    /**
     * In Seconds
     */
    protected final int timeout;
    protected final String apiKey;
    protected final String aiModel;
    protected final String TEMPLATE = """
            {{input}}.
            {{format}}.
            {{length}}.
            """;

    /**
     * Proxy settings.
     */
    protected boolean proxyEnabled = false;
    protected String proxyUrl;
    protected String proxyType;
    protected String proxyHost;
    protected int proxyPort;
    protected String proxyUser;
    protected String proxyPassword;

    public BaseLlmProvider(String apiKey, String aiModel) {
        this.apiKey = apiKey;
        this.aiModel = aiModel;
        FxPreferences fxPreferences = FxPreferences.getInstance();
        this.timeout = fxPreferences.getPreference(GEN_AI_TIMEOUT, 60);
        // Proxy settings
        proxyEnabled = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_ENABLE, false);
        if (proxyEnabled) {
            proxyType = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_TYPE, "HTTP");
            proxyHost = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_HOST, "");
            proxyPort = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_PORT, 0);
            proxyUrl = "%s://%s:%s".formatted(StringUtils.lowerCase(proxyType), proxyHost, proxyPort).trim();
            proxyUser = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_USERNAME, "");
            proxyPassword = fxPreferences.getPreference(PrefConstants.GENERAL_PROXY_PASSWORD, "");
        }
    }


    protected Map<String, Object> formatParams(String input, OutputParams outputParams) {
        HashMap<String, Object> params = new HashMap<>() {
            {
                put("input", input);
                put("format", outputParams.outputFormat() != null && StringUtils.isNotBlank(outputParams.outputFormat().getName())
                        ? "your output must be in format: %s".formatted(outputParams.outputFormat().getName())
                        : StringUtils.EMPTY);
                put("length", outputParams.outputAdjust() == null ? StringUtils.EMPTY :
                        "output : " + (outputParams.outputAdjust() == GenAiConstants.OutputAdjust.SHORTER ? "concisely" : "detailed"));
            }
        };
        return params;
    }

}
