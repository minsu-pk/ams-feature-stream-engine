package com.kbank.ams.featurestreamengine.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;

public class JsonUtil {
    public static ObjectMapper om = new ObjectMapper();

    @SneakyThrows
    public static String mapToJsonStr(Map<String,String> m) {return om.writeValueAsString(m); }

    @SneakyThrows
    public static String objMapToJsonStr(Map<String,Object> m) { return om.writeValueAsString(m); }

//    @SneakyThrows
//    public static String mapToJsonStrWithFieldSpecs(Map<String,Object> m, List<TaskModel.FileFieldSpec> fieldSpecs) {
//        Map<String,Object> convertedMap = new HashMap<>();
//        for (TaskModel.FileFieldSpec fieldSpec : fieldSpecs) {
//            switch (fieldSpec.getType()) {
//                case STRING -> convertedMap.put(fieldSpec.getName(), m.get(fieldSpec.getName()) != null ? m.get(fieldSpec.getName()).toString() : null);
//                case LOCAL_DATETIME -> convertedMap.put(fieldSpec.getName(), m.get(fieldSpec.getName()) != null ? ((LocalDateTime) m.get(fieldSpec.getName())).format(fieldSpec.getDtFormatter()) : null);
//                case INTEGER,LONG -> convertedMap.put(fieldSpec.getName(), m.get(fieldSpec.getName()) != null ? m.get(fieldSpec.getName()) : null);
//            }
//        }
//        return om.writeValueAsString(convertedMap);
//    }

    @SneakyThrows
    public static Map<String,String> jsonStrToMap(String jsonStr) {return om.readValue(jsonStr, new TypeReference<Map<String, String>>() {});}

    @SneakyThrows
    public static String removeNullFields(String jsonStr) {
        JsonNode root = om.readTree(jsonStr);
        cleanNode(root);
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void cleanNode(JsonNode node) {
        if(node.isObject()) {
            cleanObject((ObjectNode) node);
        } else if (node.isArray()) {
            cleanArray((ArrayNode) node);
        }
    }

    private static void cleanObject(ObjectNode obj) {
        List<String> toRemove = new ArrayList<>();
        Iterator<Map.Entry<String,JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String,JsonNode> entry = fields.next();
            JsonNode child = entry.getValue();

            if (child.isNull()) {
                toRemove.add(entry.getKey());
            } else {
                cleanNode(child);
            }
        }
        toRemove.forEach(obj::remove);
    }

    private static void cleanArray(ArrayNode arr) {
        for ( int i = arr.size()-1; i>=0; i-- ) {
            JsonNode element = arr.get(i);
            if (element.isNull()) {
                arr.remove(i);
            } else {
                cleanNode(element);
            }
        }
    }
}
