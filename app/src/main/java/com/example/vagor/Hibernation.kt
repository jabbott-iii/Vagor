package com.example.vagor

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.vagor.entities.Vacation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

// From Android 12, Android hibernates an app that hasn't been opened for about three months: it
// stops the app, which cancels every alarm, and they're set again only when the app is next
// opened. A reminder that fires doesn't count as use. The user can turn this off for Vagor in its
// App info, under the unused app settings. The switch's name differs between Android versions
// ("Manage app if unused" on API 37), so the app's text doesn't name it.

// Whether Android may hibernate Vagor. On Android 11 an unused app only loses its runtime
// permissions, and Vagor has none there. Android never hibernates apps in a work profile. It
// doesn't on a company-managed phone either, or where the phone's maker turned hibernation off,
// but no public API says so, so the app's text says Android "may" pause Vagor.
fun Context.mayHibernate(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
    !packageManager.isAutoRevokeWhitelisted &&
    getSystemService(UserManager::class.java)?.isManagedProfile != true

// Opens Vagor's App info screen, where the unused app setting is.
fun Context.openAppInfo() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    } catch (_: ActivityNotFoundException) {
        // Nothing to open.
    }
}

// Offered after saving a vacation that may outlast Vagor going unused
// (ReminderTimes.mayOutlastHibernation). Whichever way it's closed, the vacation form behind it
// closes too, as it does after any save.
class HibernationOfferDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.hibernation_offer_title)
        .setMessage(R.string.hibernation_offer_message)
        .setPositiveButton(R.string.open_settings) { _, _ ->
            requireContext().openAppInfo()
            requireActivity().finish()
        }
        .setNegativeButton(R.string.not_now) { _, _ -> requireActivity().finish() }
        .create()

    // Back, or a tap outside the dialog.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity?.finish()
    }

    companion object {
        private const val TAG = "hibernationOffer"

        // Offered once per app launch, so saving more vacations doesn't ask again.
        internal var offered = false

        // Shows the offer when Android may hibernate Vagor before [saved] ends. False when it
        // isn't shown. Whether notifications are on doesn't matter: on Android 13+ Vagor first
        // asks for them on the dashboard, after the first trip is saved, which is when this offer
        // matters most.
        fun showAfterSave(manager: FragmentManager, context: Context, saved: Vacation): Boolean {
            val wanted = !offered && context.mayHibernate() &&
                ReminderTimes.mayOutlastHibernation(saved.endDate, LocalDate.now()) &&
                !manager.isStateSaved && manager.findFragmentByTag(TAG) == null
            if (!wanted) return false
            offered = true
            HibernationOfferDialog().show(manager, TAG)
            return true
        }
    }
}
