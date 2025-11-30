package com.kbank.ams.featurestreamengine.common.util;

import java.util.List;
import java.util.Map;

public class ReplaceUtil {
    public static String replaceWithMap(String template, List<String> keys, Map<String,Object> m) {
        String replacedStr = new String(template);
        for (String key : keys) {
            if(m.get(key)==null) continue;
            replacedStr = replacedStr.replaceAll("\\{\\{__" + key + "__\\}\\}}", m.get(key).toString());
        }
        return replacedStr;
    }

    public static String replaceWithStr(String template, String key, String value) {
        return template.replaceAll("\\{\\{__" + key + "__\\}\\}", value);
    }
}
