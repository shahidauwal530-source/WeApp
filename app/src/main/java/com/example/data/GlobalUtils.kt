package com.example.data

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class CountryInfo(
    val code: String,
    val name: String,
    val flag: String,
    val dialCode: String,
    val currencyCode: String,
    val currencySymbol: String,
    val ratePer100Coins: Double,
    val ngnConversionRate: Double,
    val defaultCity: String
)

object GlobalCountries {
    val all = listOf(
        CountryInfo("NG", "Nigeria", "🇳🇬", "+234", "NGN", "₦", 500.0, 1.0, "Lagos"),
        CountryInfo("GH", "Ghana", "🇬🇭", "+233", "GHS", "GH₵", 7.5, 0.0095, "Accra"),
        CountryInfo("KE", "Kenya", "🇰🇪", "+254", "KES", "KSh", 65.0, 0.082, "Nairobi"),
        CountryInfo("US", "United States", "🇺🇸", "+1", "USD", "$", 0.50, 0.00063, "New York"),
        CountryInfo("GB", "United Kingdom", "🇬🇧", "+44", "GBP", "£", 0.40, 0.00050, "London"),
        CountryInfo("IN", "India", "🇮🇳", "+91", "INR", "₹", 42.0, 0.053, "Mumbai"),
        CountryInfo("BR", "Brazil", "🇧🇷", "+55", "BRL", "R$", 2.80, 0.0036, "São Paulo"),
        CountryInfo("ZA", "South Africa", "🇿🇦", "+27", "ZAR", "R", 9.0, 0.011, "Johannesburg"),
        CountryInfo("FR", "France", "🇫🇷", "+33", "EUR", "€", 0.46, 0.00058, "Paris"),
        CountryInfo("DE", "Germany", "🇩🇪", "+49", "EUR", "€", 0.46, 0.00058, "Berlin"),
        CountryInfo("ES", "Spain", "🇪🇸", "+34", "EUR", "€", 0.46, 0.00058, "Madrid"),
        CountryInfo("SA", "Saudi Arabia", "🇸🇦", "+966", "SAR", "﷼", 1.88, 0.0024, "Riyadh"),
        CountryInfo("TZ", "Tanzania", "🇹🇿", "+255", "TZS", "TSh", 1300.0, 1.65, "Dar es Salaam"),
        CountryInfo("CA", "Canada", "🇨🇦", "+1", "CAD", "C$", 0.68, 0.00086, "Toronto")
    )

    fun byCode(code: String?): CountryInfo =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: all.first { it.code == "US" }

    fun detectDeviceCountry(): CountryInfo {
        val deviceCode = Locale.getDefault().country
        return all.firstOrNull { it.code.equals(deviceCode, ignoreCase = true) }
            ?: CountryInfo("US", "United States", "🇺🇸", "+1", "USD", "$", 0.50, 0.00063, "New York")
    }

    fun isAtLeast13YearsOld(dobInput: String): Boolean {
        val clean = dobInput.trim()
        if (!clean.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = fmt.parse(clean) ?: return false
            val dobCal = Calendar.getInstance().apply { time = date }
            val cutoff = Calendar.getInstance().apply { add(Calendar.YEAR, -13) }
            !dobCal.after(cutoff)
        } catch (_: Exception) {
            false
        }
    }
}

object GlobalLanguages {
    val supported = listOf(
        "en" to "English",
        "ha" to "Hausa",
        "pcm" to "Pidgin",
        "fr" to "Français",
        "ar" to "العربية",
        "es" to "Español",
        "pt" to "Português",
        "hi" to "हिन्दी",
        "sw" to "Kiswahili"
    )

    fun detectDeviceLanguage(): String {
        val code = Locale.getDefault().language.lowercase(Locale.ROOT)
        return supported.firstOrNull { it.first == code }?.first ?: "en"
    }

    fun labelFor(code: String): String =
        supported.firstOrNull { it.first == code }?.second ?: "English"
}

object GlobalFormatters {
    fun formatRelativeTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        val now = System.currentTimeMillis()
        val diffMs = (now - date.time).coerceAtLeast(0L)
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "$mins min${if (mins == 1L) "" else "s"} ago"
            hours < 24 -> "$hours hr${if (hours == 1L) "" else "s"} ago"
            days == 1L -> {
                val calNow = Calendar.getInstance(TimeZone.getDefault())
                val calThen = Calendar.getInstance(TimeZone.getDefault()).apply { time = date }
                if (calNow.get(Calendar.DAY_OF_YEAR) - calThen.get(Calendar.DAY_OF_YEAR) <= 1) "Yesterday"
                else SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }.format(date)
            }
            days < 7 -> "$days days ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }.format(date)
        }
    }

    fun formatCoinsValue(coins: Long, country: CountryInfo): String {
        val value = (coins / 100.0) * country.ratePer100Coins
        return if (value >= 100.0) {
            "${country.currencySymbol}${String.format(Locale.US, "%,.0f", value)} (${country.currencyCode})"
        } else {
            "${country.currencySymbol}${String.format(Locale.US, "%.2f", value)} (${country.currencyCode})"
        }
    }

    fun formatNgnInLocalCurrency(amountNgn: Long, country: CountryInfo): String {
        if (country.code == "NG") {
            return "₦${String.format(Locale.US, "%,d", amountNgn)}"
        }
        val converted = amountNgn * country.ngnConversionRate
        return if (converted >= 100.0) {
            "${country.currencySymbol}${String.format(Locale.US, "%,.0f", converted)}"
        } else {
            "${country.currencySymbol}${String.format(Locale.US, "%.2f", converted)}"
        }
    }
}

object CloudinaryUploader {
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        cloudName: String,
        uploadPreset: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanCloud = cloudName.trim()
        val cleanPreset = uploadPreset.trim()
        if (cleanCloud.isEmpty() || cleanPreset.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Enter your Cloudinary Cloud Name and Unsigned Upload Preset in Account Settings to upload to Cloudinary.")
            )
        }
        try {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalStateException("Unable to read selected image"))

            val boundary = "----WeAppBoundary${System.currentTimeMillis()}"
            val url = URL("https://api.cloudinary.com/v1_1/$cleanCloud/image/upload")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(conn.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                out.writeBytes("$cleanPreset\r\n")

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"weapp_upload.jpg\"\r\n")
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("Cloudinary upload error ($code): $body"))
            }
            val secureUrl = JSONObject(body).optString("secure_url", "")
            if (secureUrl.isBlank()) {
                Result.failure(IllegalStateException("Cloudinary did not return secure_url"))
            } else {
                Result.success(secureUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
