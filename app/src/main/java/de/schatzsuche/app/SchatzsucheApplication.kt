package de.schatzsuche.app

import android.app.Application
import de.schatzsuche.app.data.repository.SchatzsucheRepository

class SchatzsucheApplication : Application() {
    lateinit var repository: SchatzsucheRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SchatzsucheRepository(this)
    }
}
