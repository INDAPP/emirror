package it.droidcon.emirror

import it.droidcon.emirror.model.AuthCode
import it.droidcon.emirror.model.Scores
import it.droidcon.emirror.model.Track
import retrofit2.http.*
import rx.Observable

/**
 * This class is part of Emirror project.
 * Created by riccardopizzoni on 09/04/17.
 * Copyright © 2017 INDAPP
 * info@indapp.it
 */
interface EmirrorApi {
    @Headers("Content-Type: application/json")
    @POST("emotion")
    fun emotions(@Path("jsessionid") session: String, @Body scores: Scores) : Observable<List<Track>>

    @GET("code")
    fun code(@Path("jsessionid") session: String) : Observable<AuthCode>
}