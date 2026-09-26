/** Handing a house's invite code to someone: the system share sheet, or the clipboard. */
package `in`.xroden.flockr.features.house.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

private const val INVITE_LINK_PREFIX = "flockr://invite/"

/** Opens the share sheet with a message that works whether or not the other person has Flockr yet. */
fun shareInvite(context: Context, houseName: String, code: String) {
    val text = "Join $houseName on Flockr: $INVITE_LINK_PREFIX$code\n\nOr open Flockr, tap Join with a code and enter $code."
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share invite"))
}

fun copyInviteCode(context: Context, code: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Invite code", code))
}
