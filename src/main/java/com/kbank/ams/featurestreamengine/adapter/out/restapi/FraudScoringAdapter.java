package com.kbank.ams.featurestreamengine.adapter.out.restapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kbank.ams.featurestreamengine.adapter.out.restapi.retrofit.RestApiServiceRegistry;
import com.kbank.ams.featurestreamengine.application.port.out.FraudScoringPort;
import com.kbank.ams.featurestreamengine.common.annotations.RestApiAdapter;
import com.kbank.ams.featurestreamengine.common.retrofit.RetrofitApiUtils;
import com.kbank.ams.featurestreamengine.common.util.JsonUtil;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringInput;
import com.kbank.ams.featurestreamengine.domain.fraudscoring.FraudScoringOutput;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import retrofit2.Call;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@RestApiAdapter
public class FraudScoringAdapter implements FraudScoringPort {
    private final RestApiServiceRegistry apiServiceRegistry;
    private final RetrofitApiUtils retrofitApiUtils;

    @Value("${ams.feature-stream-engine.ml-api.base-url}") String baseUrl;
    @Value("${ams.feature-stream-engine.ml-api.context-path}") String contextPath;

    @SneakyThrows
    @Override
    public List<FraudScoringOutput> score(List<FraudScoringInput> inputs) {
        Map<String, FraudScoringInput> fsiMap = inputs.stream().collect(
                Collectors.toMap(
                        FraudScoringInput::getUuid,
                        Function.identity()
                )
        );

        String json = JsonUtil.om.writeValueAsString(inputs);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Call<ResponseBody> call = apiServiceRegistry.api(baseUrl).post(contextPath, requestBody);

        Optional<ResponseBody> res = retrofitApiUtils.responseSync(call);
        List<FraudScoringOutput> outputs = JsonUtil.om.readValue(res.get().string(), new TypeReference<List<FraudScoringOutput>>() {});
        outputs.forEach(output->{
            String uuid = output.getUuid();
            output.setFeatures(fsiMap.get(uuid).getFeatures());
        });
        return outputs;
    }
}
