package fr.husi.bg

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import fr.husi.lib.R
import fr.husi.database.DataStore
import fr.husi.database.SagerDatabase
import fr.husi.ktx.onIoDispatcher
import fr.husi.repository.resolveRepository
import fr.husi.resources.*
import fr.husi.ui.tools.IconPackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.service.quicksettings.TileService as BaseTileService

@RequiresApi(24)
class TileService : BaseTileService() {
    private fun loadIconRest(): Icon =
        IconPackManager.loadIconBitmap(this, "ic_service_rest.png")?.let {
            Icon.createWithBitmap(it)
        } ?: Icon.createWithResource(this, R.drawable.ic_service_rest)

    private fun loadIconConnected(): Icon =
        IconPackManager.loadIconBitmap(this, "ic_service_active.png")?.let {
            Icon.createWithBitmap(it)
        } ?: Icon.createWithResource(this, R.drawable.ic_service_active)

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun attachBaseContext(newBase: Context) {
        val languageContext = ContextCompat.getContextForLanguage(newBase)
        super.attachBaseContext(languageContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        if (isLocked) unlockAndRun(this::toggle) else toggle()
    }

    private fun updateTile(serviceState: ServiceState, profileName: String?) {
        qsTile?.apply {
            label = null
            when (serviceState) {
                ServiceState.Connecting -> {
                    icon = loadIconRest()
                    state = Tile.STATE_ACTIVE
                }

                ServiceState.Connected -> {
                    icon = loadIconConnected()
                    label = profileName
                    state = Tile.STATE_ACTIVE
                }

                ServiceState.Stopping -> {
                    icon = loadIconRest()
                    state = Tile.STATE_UNAVAILABLE
                }

                // Stopped
                else -> {
                    icon = loadIconRest()
                    state = Tile.STATE_INACTIVE
                }
            }
            label = label ?: runBlocking {
                resolveRepository().getString(Res.string.app_name)
            }
            updateTile()
        }
    }

    private fun toggle() {
        scope.launch {
            val state = DataStore.serviceState
            when {
                state.canStop -> {
                    updateTile(ServiceState.Stopping, null)
                    resolveRepository().stopService()
                }

                state == ServiceState.Stopped || state == ServiceState.Idle -> {
                    updateTile(ServiceState.Connecting, null)
                    resolveRepository().startService()
                }
            }
        }
    }

    private fun refreshTile() {
        scope.launch {
            val state = DataStore.serviceState
            val profileName = if (state.connected) {
                onIoDispatcher {
                    val profileId = DataStore.currentProfile
                    if (profileId <= 0L) {
                        null
                    } else {
                        SagerDatabase.proxyDao.getById(profileId)?.let {
                            it.displayNameForService()
                        }
                    }
                }
            } else {
                null
            }
            updateTile(state, profileName)
        }
    }
}
