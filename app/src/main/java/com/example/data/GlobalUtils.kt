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
    val defaultCity: String,
    val defaultState: String,
    val states: List<String>
)

object GlobalCountries {
    val all = listOf(
        CountryInfo(
            code = "NG",
            name = "Nigeria",
            flag = "🇳🇬",
            dialCode = "+234",
            currencyCode = "NGN",
            currencySymbol = "₦",
            ratePer100Coins = 500.0,
            ngnConversionRate = 1.0,
            defaultCity = "Lagos",
            defaultState = "Lagos",
            states = listOf(
                "Abia", "Adamawa", "Akwa Ibom", "Anambra", "Bauchi", "Bayelsa", "Benue",
                "Borno", "Cross River", "Delta", "Ebonyi", "Edo", "Ekiti", "Enugu",
                "FCT Abuja", "Gombe", "Imo", "Jigawa", "Kaduna", "Kano", "Katsina",
                "Kebbi", "Kogi", "Kwara", "Lagos", "Nasarawa", "Niger", "Ogun", "Ondo",
                "Osun", "Oyo", "Plateau", "Rivers", "Sokoto", "Taraba", "Yobe", "Zamfara"
            )
        ),
        CountryInfo(
            code = "GH",
            name = "Ghana",
            flag = "🇬🇭",
            dialCode = "+233",
            currencyCode = "GHS",
            currencySymbol = "GH₵",
            ratePer100Coins = 7.5,
            ngnConversionRate = 0.0095,
            defaultCity = "Accra",
            defaultState = "Greater Accra",
            states = listOf(
                "Ahafo", "Ashanti", "Bono", "Bono East", "Central", "Eastern",
                "Greater Accra", "North East", "Northern", "Oti", "Savannah",
                "Upper East", "Upper West", "Volta", "Western", "Western North"
            )
        ),
        CountryInfo(
            code = "KE",
            name = "Kenya",
            flag = "🇰🇪",
            dialCode = "+254",
            currencyCode = "KES",
            currencySymbol = "KSh",
            ratePer100Coins = 65.0,
            ngnConversionRate = 0.082,
            defaultCity = "Nairobi",
            defaultState = "Nairobi",
            states = listOf(
                "Baringo", "Bomet", "Bungoma", "Busia", "Elgeyo-Marakwet", "Embu", "Garissa",
                "Homa Bay", "Isiolo", "Kajiado", "Kakamega", "Kericho", "Kiambu", "Kilifi",
                "Kirinyaga", "Kisii", "Kisumu", "Kitui", "Kwale", "Laikipia", "Lamu",
                "Machakos", "Makueni", "Mandera", "Marsabit", "Meru", "Migori", "Mombasa",
                "Murang'a", "Nairobi", "Nakuru", "Nandi", "Narok", "Nyamira", "Nyandarua",
                "Nyeri", "Samburu", "Siaya", "Taita-Taveta", "Tana River", "Tharaka-Nithi",
                "Trans Nzoia", "Turkana", "Uasin Gishu", "Vihiga", "Wajir", "West Pokot"
            )
        ),
        CountryInfo(
            code = "US",
            name = "United States",
            flag = "🇺🇸",
            dialCode = "+1",
            currencyCode = "USD",
            currencySymbol = "$",
            ratePer100Coins = 0.50,
            ngnConversionRate = 0.00063,
            defaultCity = "New York",
            defaultState = "New York",
            states = listOf(
                "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
                "Connecticut", "Delaware", "District of Columbia", "Florida", "Georgia",
                "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky",
                "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
                "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
                "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota",
                "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
                "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia",
                "Washington", "West Virginia", "Wisconsin", "Wyoming"
            )
        ),
        CountryInfo(
            code = "GB",
            name = "United Kingdom",
            flag = "🇬🇧",
            dialCode = "+44",
            currencyCode = "GBP",
            currencySymbol = "£",
            ratePer100Coins = 0.40,
            ngnConversionRate = 0.00050,
            defaultCity = "London",
            defaultState = "Greater London",
            states = listOf(
                "England", "Scotland", "Wales", "Northern Ireland", "Greater London",
                "Greater Manchester", "West Midlands", "West Yorkshire", "Merseyside",
                "Kent", "Essex", "Hampshire", "Surrey", "Lancashire"
            )
        ),
        CountryInfo(
            code = "IN",
            name = "India",
            flag = "🇮🇳",
            dialCode = "+91",
            currencyCode = "INR",
            currencySymbol = "₹",
            ratePer100Coins = 42.0,
            ngnConversionRate = 0.053,
            defaultCity = "Mumbai",
            defaultState = "Maharashtra",
            states = listOf(
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chandigarh",
                "Chhattisgarh", "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
                "Jammu and Kashmir", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
                "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha",
                "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
                "Uttar Pradesh", "Uttarakhand", "West Bengal"
            )
        ),
        CountryInfo(
            code = "BR",
            name = "Brazil",
            flag = "🇧🇷",
            dialCode = "+55",
            currencyCode = "BRL",
            currencySymbol = "R$",
            ratePer100Coins = 2.80,
            ngnConversionRate = 0.0036,
            defaultCity = "São Paulo",
            defaultState = "São Paulo",
            states = listOf(
                "Acre", "Alagoas", "Amapá", "Amazonas", "Bahia", "Ceará",
                "Distrito Federal", "Espírito Santo", "Goiás", "Maranhão", "Mato Grosso",
                "Mato Grosso do Sul", "Minas Gerais", "Pará", "Paraíba", "Paraná",
                "Pernambuco", "Piauí", "Rio de Janeiro", "Rio Grande do Norte",
                "Rio Grande do Sul", "Rondônia", "Roraima", "Santa Catarina",
                "São Paulo", "Sergipe", "Tocantins"
            )
        ),
        CountryInfo(
            code = "ZA",
            name = "South Africa",
            flag = "🇿🇦",
            dialCode = "+27",
            currencyCode = "ZAR",
            currencySymbol = "R",
            ratePer100Coins = 9.0,
            ngnConversionRate = 0.011,
            defaultCity = "Johannesburg",
            defaultState = "Gauteng",
            states = listOf(
                "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
                "Mpumalanga", "North West", "Northern Cape", "Western Cape"
            )
        ),
        CountryInfo(
            code = "FR",
            name = "France",
            flag = "🇫🇷",
            dialCode = "+33",
            currencyCode = "EUR",
            currencySymbol = "€",
            ratePer100Coins = 0.46,
            ngnConversionRate = 0.00058,
            defaultCity = "Paris",
            defaultState = "Île-de-France",
            states = listOf(
                "Auvergne-Rhône-Alpes", "Bourgogne-Franche-Comté", "Bretagne",
                "Centre-Val de Loire", "Corse", "Grand Est", "Hauts-de-France",
                "Île-de-France", "Normandie", "Nouvelle-Aquitaine", "Occitanie",
                "Pays de la Loire", "Provence-Alpes-Côte d'Azur"
            )
        ),
        CountryInfo(
            code = "DE",
            name = "Germany",
            flag = "🇩🇪",
            dialCode = "+49",
            currencyCode = "EUR",
            currencySymbol = "€",
            ratePer100Coins = 0.46,
            ngnConversionRate = 0.00058,
            defaultCity = "Berlin",
            defaultState = "Berlin",
            states = listOf(
                "Baden-Württemberg", "Bavaria", "Berlin", "Brandenburg", "Bremen",
                "Hamburg", "Hesse", "Lower Saxony", "Mecklenburg-Vorpommern",
                "North Rhine-Westphalia", "Rhineland-Palatinate", "Saarland", "Saxony",
                "Saxony-Anhalt", "Schleswig-Holstein", "Thuringia"
            )
        ),
        CountryInfo(
            code = "ES",
            name = "Spain",
            flag = "🇪🇸",
            dialCode = "+34",
            currencyCode = "EUR",
            currencySymbol = "€",
            ratePer100Coins = 0.46,
            ngnConversionRate = 0.00058,
            defaultCity = "Madrid",
            defaultState = "Community of Madrid",
            states = listOf(
                "Andalusia", "Aragon", "Asturias", "Balearic Islands", "Basque Country",
                "Canary Islands", "Cantabria", "Castile and León", "Castilla-La Mancha",
                "Catalonia", "Community of Madrid", "Extremadura", "Galicia", "La Rioja",
                "Navarre", "Region of Murcia", "Valencian Community"
            )
        ),
        CountryInfo(
            code = "SA",
            name = "Saudi Arabia",
            flag = "🇸🇦",
            dialCode = "+966",
            currencyCode = "SAR",
            currencySymbol = "﷼",
            ratePer100Coins = 1.88,
            ngnConversionRate = 0.0024,
            defaultCity = "Riyadh",
            defaultState = "Riyadh",
            states = listOf(
                "Al Bahah", "Al Jouf", "Asir", "Eastern Province", "Hail", "Jazan",
                "Madinah", "Makkah", "Najran", "Northern Borders", "Qassim",
                "Riyadh", "Tabuk"
            )
        ),
        CountryInfo(
            code = "TZ",
            name = "Tanzania",
            flag = "🇹🇿",
            dialCode = "+255",
            currencyCode = "TZS",
            currencySymbol = "TSh",
            ratePer100Coins = 1300.0,
            ngnConversionRate = 1.65,
            defaultCity = "Dar es Salaam",
            defaultState = "Dar es Salaam",
            states = listOf(
                "Arusha", "Dar es Salaam", "Dodoma", "Geita", "Iringa", "Kagera",
                "Katavi", "Kigoma", "Kilimanjaro", "Lindi", "Manyara", "Mara", "Mbeya",
                "Morogoro", "Mtwara", "Mwanza", "Njombe", "Pemba North", "Pemba South",
                "Pwani", "Rukwa", "Ruvuma", "Shinyanga", "Simiyu", "Singida", "Songwe",
                "Tabora", "Tanga", "Zanzibar Central/South", "Zanzibar North",
                "Zanzibar Urban/West"
            )
        ),
        CountryInfo(
            code = "CA",
            name = "Canada",
            flag = "🇨🇦",
            dialCode = "+1",
            currencyCode = "CAD",
            currencySymbol = "C$",
            ratePer100Coins = 0.68,
            ngnConversionRate = 0.00086,
            defaultCity = "Toronto",
            defaultState = "Ontario",
            states = listOf(
                "Alberta", "British Columbia", "Manitoba", "New Brunswick",
                "Newfoundland and Labrador", "Northwest Territories", "Nova Scotia",
                "Nunavut", "Ontario", "Prince Edward Island", "Quebec", "Saskatchewan",
                "Yukon"
            )
        )
    )

    fun byCode(code: String?): CountryInfo =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: all.first { it.code == "US" }

    fun statesFor(code: String?): List<String> = byCode(code).states

    fun detectDeviceCountry(): CountryInfo {
        val deviceCode = Locale.getDefault().country
        return all.firstOrNull { it.code.equals(deviceCode, ignoreCase = true) }
            ?: all.first { it.code == "US" }
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
