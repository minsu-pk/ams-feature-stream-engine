package com.kbank.ams.featurestreamengine.common.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
public class ResourceFileUtil {
    public static String load(String filePath){
        String loadedText = null;
        try{
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)
            );
            loadedText = reader.lines().collect(Collectors.joining("\n"));
        }catch (Exception e){
            try{
                Resource resource = new ClassPathResource(filePath);
                loadedText = new String(Files.readAllBytes(resource.getFile().toPath()), StandardCharsets.UTF_8);
            }catch (Exception e2){
                log.warn("Failed to load File {}", e2);
            }
        }
        //log.info("loadedText: {}", loadedText);
        return loadedText;
    }
}
