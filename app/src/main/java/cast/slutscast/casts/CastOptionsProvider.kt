package cast.slutscast.casts

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

class CastOptionsProvider : OptionsProvider {
    private var notificationOptions = NotificationOptions.Builder()
        .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
        .build()
    private var mediaOptions = CastMediaOptions.Builder()
        .setNotificationOptions(notificationOptions)
        .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
        .build()
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId("F52D6E5E")
            .setResumeSavedSession(true)
            .setEnableReconnectionService(true)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}