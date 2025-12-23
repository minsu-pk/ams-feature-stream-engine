package com.kbank.ams.featurestreamengine.common.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RetrofitApiUtils {

    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1,TimeUnit.SECONDS)
            .writeTimeout(1,TimeUnit.SECONDS);

    private static final Gson gson = new GsonBuilder()
            .setLenient()
            .create();

    public static Retrofit initRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
    }

    public <T> Optional<T> responseSync(Call<T> call) {
        Response<T> response = null;

        try{
            log.info("[===== Retrofit call git");
            response = call.execute();
        } catch (IOException e) {
            log.warn("cannot connect.");
        }

        if(response!=null && response.isSuccessful()) {
            return Optional.ofNullable(response.body());
        }else{
            log.warn("{}", response!=null ? response.errorBody() : "response is null");
            return null;
        }
    }
}