package com.kbank.ams.featurestreamengine.application.specrepo;

import com.kbank.ams.featurestreamengine.common.util.ResourceFileUtil;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ProcessSpecRepoConfig {

    @Primary
    @Bean
    public Map<String,String> flowSpecRepo(
        @Value("${ams.feature-stream-engine.config-dir}") String configDir,
        @Value("${ams.feature-stream-engine.flow-list-path}") String flowListPath
    ){
        // 1) 태스크 목록 읽기: CRLF 정리, trim, 공백/주석 제거
        List<String> names = Arrays.stream(ResourceFileUtil.load(flowListPath)
                .replace("\r", "")
                .split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith("#"))
            .toList();

        // 2) key=태스크명, value=spec.json 내용 으로 맵 구성
        return IntStream.range(0, names.size())
            .boxed()
            .collect(Collectors.toMap(
                names::get,
                i -> ResourceFileUtil.load(configDir + "/flow/" + names.get(i) + "/spec.json"),
                (a, b) -> b,                 // key 충돌 시 뒤 값 사용
                LinkedHashMap::new           // 입력 순서 유지
            ));
    }

    @Primary
    @Bean
    public List<String> flowNames(
        @Value("${ams.feature-stream-engine.flow-list-path}") String flowListPath
    ){
        return Arrays.stream(ResourceFileUtil.load(flowListPath)
                .replace("\r", "")
                .split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith("#"))
            .toList();
    }
}
