package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppGradientButton
import com.example.ui.components.WeSecondaryTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    currentUser: PrivateUser,
    cloudinaryCloudName: String,
    cloudinaryUploadPreset: String,
    isSubmitting: Boolean,
    strings: AppStrings,
    onSaveCloudinaryConfig: (cloudName: String, preset: String) -> Unit,
    onSaveAccount: (
        displayName: String,
        username: String,
        email: String,
        phone: String,
        dateOfBirth: String,
        gender: String,
        country: CountryInfo,
        city: String,
        bio: String,
        website: String,
        work: String,
        education: String,
        relationshipStatus: String,
        photoUrl: String,
        coverPhotoUrl: String
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember(currentUser.userId) { mutableStateOf(currentUser.displayName) }
    var username by remember(currentUser.userId) { mutableStateOf(currentUser.username) }
    var email by remember(currentUser.userId) { mutableStateOf(currentUser.email) }
    var phone by remember(currentUser.userId) { mutableStateOf(currentUser.phone) }
    var dob by remember(currentUser.userId) { mutableStateOf(currentUser.dateOfBirth) }
    var gender by remember(currentUser.userId) { mutableStateOf(currentUser.gender) }
    var selectedCountry by remember(currentUser.userId) { mutableStateOf(GlobalCountries.byCode(currentUser.countryCode)) }
    var city by remember(currentUser.userId) { mutableStateOf(currentUser.city) }
    var bio by remember(currentUser.userId) { mutableStateOf(currentUser.bio) }
    var website by remember(currentUser.userId) { mutableStateOf(currentUser.website) }
    var work by remember(currentUser.userId) { mutableStateOf(currentUser.work) }
    var education by remember(currentUser.userId) { mutableStateOf(currentUser.education) }
    var relationshipStatus by remember(currentUser.userId) { mutableStateOf(currentUser.relationshipStatus) }
    var photoUrl by remember(currentUser.userId) { mutableStateOf(currentUser.photoUrl) }
    var coverPhotoUrl by remember(currentUser.userId) { mutableStateOf(currentUser.coverPhotoUrl) }

    var cloudNameInput by remember(cloudinaryCloudName) { mutableStateOf(cloudinaryCloudName) }
    var uploadPresetInput by remember(cloudinaryUploadPreset) { mutableStateOf(cloudinaryUploadPreset) }
    var uploadingMedia by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSaveCloudinaryConfig(cloudNameInput, uploadPresetInput)
            if (cloudNameInput.isNotBlank() && uploadPresetInput.isNotBlank()) {
                uploadingMedia = true
                scope.launch {
                    val result = CloudinaryUploader.uploadImage(context, uri, cloudNameInput, uploadPresetInput)
                    uploadingMedia = false
                    result.fold(
                        onSuccess = { url ->
                            photoUrl = url
                            onShowMessage("Profile photo uploaded to Cloudinary!")
                        },
                        onFailure = { e ->
                            onShowMessage(e.localizedMessage ?: "Cloudinary upload failed")
                        }
                    )
                }
            } else {
                photoUrl = uri.toString()
                onShowMessage("Selected photo locally. Add Cloudinary Cloud Name + Preset above for cloud URL.")
            }
        }
    }

    val coverPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSaveCloudinaryConfig(cloudNameInput, uploadPresetInput)
            if (cloudNameInput.isNotBlank() && uploadPresetInput.isNotBlank()) {
                uploadingMedia = true
                scope.launch {
                    val result = CloudinaryUploader.uploadImage(context, uri, cloudNameInput, uploadPresetInput)
                    uploadingMedia = false
                    result.fold(
                        onSuccess = { url ->
                            coverPhotoUrl = url
                            onShowMessage("Cover photo uploaded to Cloudinary!")
                        },
                        onFailure = { e ->
                            onShowMessage(e.localizedMessage ?: "Cloudinary upload failed")
                        }
                    )
                }
            } else {
                coverPhotoUrl = uri.toString()
                onShowMessage("Selected cover photo locally. Add Cloudinary Cloud Name + Preset above for cloud URL.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar("Account & ${strings.editProfile}", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photos Card (Profile + Cover + Photo Picker / Cloudinary)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile & Cover Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (coverPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = coverPhotoUrl,
                            contentDescription = "Cover preview",
                            modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeAppAvatar(name = displayName.ifBlank { username }, size = 56.dp, photoUrl = photoUrl)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = {
                                    profilePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !uploadingMedia,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pick Profile Photo")
                            }
                            OutlinedButton(
                                onClick = {
                                    coverPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !uploadingMedia,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Panorama, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pick Cover Photo")
                            }
                        }
                    }
                    if (uploadingMedia) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = WePurple)
                    }
                    OutlinedTextField(
                        value = cloudNameInput,
                        onValueChange = { cloudNameInput = it },
                        label = { Text("Cloudinary Cloud Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadPresetInput,
                        onValueChange = { uploadPresetInput = it },
                        label = { Text("Cloudinary Unsigned Upload Preset (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Identity & Contact
            OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(50) }, label = { Text(strings.displayNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = username, onValueChange = { username = it.take(30) }, label = { Text(strings.usernameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it.take(120) }, label = { Text(strings.emailLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it.take(25) }, label = { Text(strings.phoneLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dob, onValueChange = { dob = it.take(30) }, label = { Text("Date of Birth (e.g. 1999-05-12)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = gender, onValueChange = { gender = it.take(30) }, label = { Text("Gender") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // Country & City
            OutlinedCard(
                onClick = { showCountryPicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.currencyCode})", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.countryLabel)
                }
            }
            OutlinedTextField(value = city, onValueChange = { city = it.take(60) }, label = { Text(strings.cityLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // Facebook-style Bio & Life Details
            OutlinedTextField(value = bio, onValueChange = { bio = it.take(200) }, label = { Text(strings.bioLabel) }, maxLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = website, onValueChange = { website = it.take(150) }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = work, onValueChange = { work = it.take(100) }, label = { Text("Work") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = education, onValueChange = { education = it.take(100) }, label = { Text("Education") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = relationshipStatus, onValueChange = { relationshipStatus = it.take(40) }, label = { Text("Relationship Status") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(6.dp))

            WeAppGradientButton(
                text = if (isSubmitting) "..." else strings.saveChanges,
                enabled = !isSubmitting && !uploadingMedia,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_account_settings_button",
                onClick = {
                    onSaveCloudinaryConfig(cloudNameInput, uploadPresetInput)
                    onSaveAccount(
                        displayName, username, email, phone, dob, gender,
                        selectedCountry, city, bio, website, work, education,
                        relationshipStatus, photoUrl, coverPhotoUrl
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            title = strings.countryLabel,
            selectedCode = selectedCountry.code,
            onDismiss = { showCountryPicker = false },
            onSelect = { info ->
                selectedCountry = info
                city = info.defaultCity
                showCountryPicker = false
            }
        )
    }
}
