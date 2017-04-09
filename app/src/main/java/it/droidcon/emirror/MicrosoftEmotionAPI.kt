package it.droidcon.emirror

import it.droidcon.emirror.model.Entry
import okhttp3.RequestBody
import retrofit2.http.*
import rx.Observable

/**
 * This class is part of Emirror project.
 * Created by riccardopizzoni on 08/04/17.
 * Copyright © 2017 INDAPP
 * info@indapp.it
 */
interface MicrosoftEmotionAPI {
    @Headers("Content-Type: application/octet-stream")
    @POST("recognize")
    fun recognize(@Body photo : RequestBody) : Observable<List<Entry>>
}