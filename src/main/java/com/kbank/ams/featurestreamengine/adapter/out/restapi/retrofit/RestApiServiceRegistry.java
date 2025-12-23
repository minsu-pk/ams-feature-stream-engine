package com.kbank.ams.featurestreamengine.adapter.out.restapi.retrofit;

import com.kbank.ams.featurestreamengine.common.retrofit.RetrofitApiUtils;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

@Component
public class RestApiServiceRegistry {
    public Api api(String baseUrl){
        Retrofit retrofit = RetrofitApiUtils.initRetrofit(baseUrl);
        return retrofit.create(Api.class);
    }
    public interface Api {
        @POST("{contextPath}")
        Call<ResponseBody> post(@Path(value = "contextPath", encoded = true) String contextPath, @Body RequestBody body);
    }
}
