package it.droidcon.emirror

import android.content.Context
import com.squareup.moshi.Moshi
import it.droidcon.emirror.model.AuthCode
import it.droidcon.emirror.model.Entry
import it.droidcon.emirror.model.Scores
import it.droidcon.emirror.model.Track
import okhttp3.OkHttpClient
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
        lateinit private var emirror: Client<EmirrorApi>

        fun init(ctx: Context) {
            val moshi = Moshi.Builder().build()
            val microsoftClient = OkHttpClient.Builder().addInterceptor {
                val request = it.request()
                val new = request.newBuilder()
                        .addHeader("Content-Type","application/json")
                        .addHeader("Ocp-Apim-Subscription-Key","3274404c1598431dbe8f160428a6fb22")
                        .build()
                it.proceed(new)
            }.build()

            val microsoftApi = Retrofit.Builder()
                    .baseUrl("https://westus.api.cognitive.microsoft.com/emotion/v1.0/")
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(microsoftClient)
                    .build()
                    .create(MicrosoftEmotionAPI::class.java)

            val emirrorApi = Retrofit.Builder()
                    .baseUrl("http://172.16.0.139:8080/api/v1/")
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build()
                    .create(EmirrorApi::class.java)

            microsoft = Client(microsoftApi)
            emirror = Client(emirrorApi)
        }

        fun recognize(photo: RequestBody) : Observable<List<Entry>> {
            return microsoft.endpoints.recognize(photo)
        }

        fun emotions(session: String, emotions: Scores) : Observable<List<Track>> {
            return emirror.endpoints.emotions(session, emotions)
        }

        fun code(session: String) : Observable<AuthCode> {
            return emirror.endpoints.code(session)
        }
    }


}
