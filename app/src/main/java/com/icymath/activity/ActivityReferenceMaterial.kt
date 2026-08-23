package com.icymath.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import com.icymath.R
import com.icymath.content.ContentDownloadViewModel
import com.icymath.items.ItemType
import com.icymath.items.LectureId
import com.icymath.items.ReferenceItem
import com.icymath.managers.ImagePicker
import com.icymath.managers.LocaleManager
import com.icymath.managers.SystemUiManager
import com.icymath.managers.ThemeManager
import com.icymath.pdf.ActivityPdfViewer
import com.icymath.ui.activity.ReferenceMaterialScreenBridge
import com.icymath.utils.SecurityUtils

class ActivityReferenceMaterial : AppCompatActivity() {

    private lateinit var composeView: ComposeView
    private val contentViewModel: ContentDownloadViewModel by viewModels()
    private var isLectureMode = false
    private var currentSubjectResId: Int? = null
    
    private val subjects = mutableListOf<Int>()
    private val lecturesMap = mutableMapOf<Int, List<LectureId>>()
    private var imagePicker: ImagePicker? = null

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleManager.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        SystemUiManager.applyEdgeToEdge(this)
        super.onCreate(savedInstanceState)
        
        SecurityUtils.checkLock(this)

        requestedOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        initData()

        composeView = ComposeView(this)
        setContentView(composeView)

        imagePicker = ImagePicker(this, object : ImagePicker.Callback {
            override fun onResult(upperLine: String?, lowerLine: String?) {
                if (!upperLine.isNullOrEmpty() && !lowerLine.isNullOrEmpty()) {
                    val intent = Intent(this@ActivityReferenceMaterial, ActivitySubstitutions::class.java).apply {
                        putExtra("upperLine", upperLine)
                        putExtra("lowerLine", lowerLine)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ActivityReferenceMaterial, getString(R.string.error_photo_camera), Toast.LENGTH_LONG).show()
                }
            }
        })
        registerLaunchersAndBindToImagePicker()

        updateUi()
        contentViewModel.enterReferenceScreen()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isLectureMode) {
                    showSubjects()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        SecurityUtils.checkLock(this)
    }

    private fun initData() {
        subjects.clear()
        subjects.add(R.string.algebra_and_number_theory)
        lecturesMap[R.string.algebra_and_number_theory] = listOf(
            LectureId.ALGEBRAIC_STRUCTURES,
            LectureId.DIVISIBILITY_IN_INTEGERS,
            LectureId.GCD_LCM,
            LectureId.PRIME_NUMBERS,
            LectureId.NUMERIC_COMPARISONS,
            LectureId.SOLVING_COMPARISONS,
            LectureId.COMPLEX_NUMBERS_1,
            LectureId.COMPLEX_NUMBERS_2,
            LectureId.SLU_GAUSS,
            LectureId.MATRICES,
            LectureId.DETERMINANTS,
            LectureId.PERMUTATIONS
        )
    }

    private fun updateUi() {
        val title: String
        val items = mutableListOf<ReferenceItem>()

        if (isLectureMode && currentSubjectResId != null) {
            title = getString(currentSubjectResId!!)
            lecturesMap[currentSubjectResId]?.forEach { lec ->
                items.add(ReferenceItem(ItemType.LECTURE, lec, lec.titleResId()))
            }
        } else {
            title = getString(R.string.reference_material)
            subjects.forEach { s ->
                items.add(ReferenceItem(ItemType.SUBJECT, null, s))
            }
        }

        ReferenceMaterialScreenBridge.setReferenceContent(
            composeView = composeView,
            title = title,
            items = items,
            onBack = {
                if (isLectureMode) showSubjects()
                else finish()
            },
            onItemClick = { item ->
                if (item.type == ItemType.SUBJECT) {
                    showLectures(item.titleResId)
                } else {
                    item.lectureId?.let { contentViewModel.openLecture(it.ordinal + 1) }
                }
            },
            onMenuAction = { id ->
                when (id) {
                    R.id.nav_reference -> { /* Мы уже здесь */ }
                    R.id.nav_camera -> imagePicker?.requestCamera()
                    R.id.nav_home -> {
                        startActivity(Intent(this, ActivitySubstitutions::class.java))
                        finish()
                    }
                    R.id.nav_gallery -> imagePicker?.startGallery()
                    R.id.nav_history -> {
                        startActivity(Intent(this, ActivityHistory::class.java))
                        finish()
                    }
                }
            },
            downloadViewModel = contentViewModel,
            onOpenPdf = ::openVerifiedPdf
        )
    }

    private fun showLectures(subjectResId: Int?) {
        isLectureMode = true
        currentSubjectResId = subjectResId
        updateUi()
    }

    private fun showSubjects() {
        isLectureMode = false
        currentSubjectResId = null
        updateUi()
    }

    private fun openVerifiedPdf(path: String) {
        val intent = Intent(this, ActivityPdfViewer::class.java).apply {
            putExtra("pdf_path", path)
        }
        startActivity(intent)
    }

    private fun registerLaunchersAndBindToImagePicker() {
        val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) imagePicker?.startCamera()
        }
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) imagePicker?.handleCameraResult()
        }
        val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) imagePicker?.handleGalleryResult(result.data)
        }
        imagePicker?.registerLaunchers(requestCameraPermissionLauncher, cameraLauncher, galleryLauncher)
    }

}
