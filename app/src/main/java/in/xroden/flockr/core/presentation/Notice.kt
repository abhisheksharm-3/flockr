/** A one-off message a screen shows in a snackbar after an action, with the haptic that matches it. */
package `in`.xroden.flockr.core.presentation

data class Notice(val message: String, val isError: Boolean)
