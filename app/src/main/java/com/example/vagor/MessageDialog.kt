package com.example.vagor

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// A message with an OK button. A DialogFragment, so it's still open after a rotation; a dialog
// shown directly closes with the screen that's recreated.
class MessageDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        var message = getString(args.getInt(ARG_MESSAGE))
        val rawTextId = args.getInt(ARG_RAW_TEXT)
        if (rawTextId != 0) {
            val rawText = resources.openRawResource(rawTextId).bufferedReader().use { it.readText() }
            message += "\n\n" + reflowParagraphs(rawText)
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
        val titleId = args.getInt(ARG_TITLE)
        if (titleId != 0) builder.setTitle(titleId)
        return builder.create()
    }

    companion object {
        private const val ARG_TITLE = "titleId"
        private const val ARG_MESSAGE = "messageId"
        private const val ARG_RAW_TEXT = "rawTextId"

        // [titleId] 0 shows no title. [rawTextId], when not 0, is a text file in res/raw whose
        // paragraphs follow the message (reflowParagraphs).
        fun show(
            manager: FragmentManager,
            tag: String,
            @StringRes titleId: Int,
            @StringRes messageId: Int,
            @RawRes rawTextId: Int = 0
        ) {
            // A second tap, or a screen reader's, while it's opening.
            if (manager.isStateSaved || manager.findFragmentByTag(tag) != null) return
            val dialog = MessageDialog()
            dialog.arguments = Bundle().apply {
                putInt(ARG_TITLE, titleId)
                putInt(ARG_MESSAGE, messageId)
                putInt(ARG_RAW_TEXT, rawTextId)
            }
            dialog.showNow(manager, tag)
        }
    }
}
