package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord

/** How a contact is shown everywhere: its name, else its email, else "Unknown contact". */
@Composable
fun contactLabel(contact: ContactRecord): String = displayNameOrUnknown(contact.displayName)

/** [displayName] (see [ContactRecord.displayName]), or "Unknown contact" when it is null. */
@Composable
fun displayNameOrUnknown(displayName: String?): String = displayName ?: stringResource(id = R.string.contact_unknown)
