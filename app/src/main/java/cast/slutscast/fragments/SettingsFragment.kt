package cast.slutscast.fragments

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import cast.slutscast.R

class SettingsFragment : PreferenceFragmentCompat() {
    private var autoPlay:Boolean = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val autoPlaySwitchPreference = findPreference<SwitchPreference>(getString(R.string.autoplay_switch))
        //autoPlaySwitchPreference?.onPreferenceClickListener = autoPlaySwitchListener
    }
}