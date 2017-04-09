package it.droidcon.emirror

import android.content.Context
import com.squareup.moshi.Moshi
import it.droidcon.emirror.model.Entry
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import rx.Observable

/**
 * This class is part of Emirror project.
 * Created by riccardopizzoni on 08/04/17.
 * Copyright © 2017 INDAPP
 * info@indapp.it
 */
class Client<T> (val endpoints : T) {
    companion object {
        lateinit private var microsoft: Client<MicrosoftEmotionAPI>

        fun init(ctx: Context) {
            val moshi = Moshi.Builder().build()
//            val microsoftClient = OkHttpClient.Builder().addInterceptor {
//                val request = it.request()
//                val new = request.newBuilder()
//                        .addHeader("Content-Type","application/json")
//                        .addHeader("Ocp-Apim-Subscription-Key","3274404c1598431dbe8f160428a6fb22")
//                        .build()
//                it.proceed(new)
//            }.build()

            val microsoftApi = Retrofit.Builder()
                    .baseUrl("https://westus.api.cognitive.microsoft.com/emotion/v1.0/")
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    //.client(microsoftClient)
                    .build()
                    .create(MicrosoftEmotionAPI::class.java)

            microsoft = Client(microsoftApi)
        }

        fun recognize(photo: RequestBody) : Observable<List<Entry>> {
            return microsoft.endpoints.recognize("3274404c1598431dbe8f160428a6fb22", photo)
        }
    }


}
