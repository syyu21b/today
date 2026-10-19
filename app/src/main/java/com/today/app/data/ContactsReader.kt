package com.today.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactEntry(val id: Long, val name: String)

/** Reads display names from the device address book. Nothing leaves the device. */
object ContactsReader {

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun read(context: Context, limit: Int = 500): List<ContactEntry> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val out = ArrayList<ContactEntry>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL",
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (c.moveToNext() && out.size < limit) {
                    val name = c.getString(nameCol)?.trim().orEmpty()
                    if (name.isNotEmpty()) out += ContactEntry(c.getLong(idCol), name)
                }
            }
        }
        return out
    }
}
