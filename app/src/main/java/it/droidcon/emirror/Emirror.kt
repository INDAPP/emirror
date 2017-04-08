package it.droidcon.emirror

import android.app.Application

/**
 * This class is part of Emirror project.
 * Created by riccardopizzoni on 08/04/17.
 * Copyright © 2017 INDAPP
 * info@indapp.it
 */
class Emirror : Application() {
    override fun onCreate() {
        super.onCreate()
        Client.init(this)
    }
}