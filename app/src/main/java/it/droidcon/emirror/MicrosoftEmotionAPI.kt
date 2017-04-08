package it.droidcon.emirror

import it.droidcon.emirror.model.Entry
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * This class is part of Emirror project.
 * Created by riccardopizzoni on 08/04/17.
 * Copyright © 2017 INDAPP
 * info@indapp.it
 */
interface MicrosoftEmotionAPI {
    @Headers("Content-Type: application/octet-stream")
    @POST("recognize")
    fun recognize(@Header("Ocp-Apim-Subscription-Key") key: String, @Body photo : RequestBody) : Observable<List<Entry>>
}