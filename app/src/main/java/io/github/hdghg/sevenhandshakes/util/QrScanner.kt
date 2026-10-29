package io.github.hdghg.sevenhandshakes.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object QrScanner {
    fun scanQr(context: Context, onSuccess: (value: String) -> Unit, onFail: (e: Exception) -> Unit) {
        val options =
            GmsBarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        GmsBarcodeScanning
            .getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue.orEmpty()
                onSuccess(value)
            }.addOnFailureListener(onFail)
    }
}
