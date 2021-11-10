package com.kdjj.presentation.view.recipeeditor

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kdjj.presentation.R
import com.kdjj.presentation.databinding.ActivityRecipeEditorBinding
import com.kdjj.presentation.model.RecipeEditorItem
import com.kdjj.presentation.view.adapter.RecipeEditorListAdapter
import com.kdjj.presentation.viewmodel.recipeeditor.RecipeEditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RecipeEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeEditorBinding
    private val viewModel: RecipeEditorViewModel by viewModels()

    private var lastCameraFileUri: String? = null
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.setImage(lastCameraFileUri)
        } else {
            viewModel.setImage(null)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.setImage(result?.data?.dataString)
        } else {
            viewModel.setImage(null)
        }
    }

    private val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPos = viewHolder.absoluteAdapterPosition
            val toPos = target.absoluteAdapterPosition
            if (target.itemViewType != RecipeEditorListAdapter.TYPE_STEP) return false
            viewModel.changeRecipeStepPosition(fromPos, toPos)
            return true
        }

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return if (viewHolder.itemViewType != RecipeEditorListAdapter.TYPE_STEP ||
                recyclerView.adapter?.itemCount == 3
            ) {
                makeMovementFlags(0, 0)
            } else {
                makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                )
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            viewModel.removeRecipeStep(viewHolder.absoluteAdapterPosition)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_recipe_editor)
        binding.lifecycleOwner = this

        binding.viewModel = viewModel

        setSupportActionBar(binding.toolbarEditor)
        setTitle(R.string.addRecipe)

        val adapter = RecipeEditorListAdapter(viewModel)
        binding.recyclerViewEditor.adapter = adapter
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerViewEditor)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.liveImgTarget.observe(this) { model ->
            model?.let { showSelectImageDialog() }
        }
    }

    private fun showSelectImageDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.selectPhoto)
            .setItems(R.array.photoSourceSelection) { _, i ->
                when (i) {
                    0 -> {
                        startCamera()
                    }
                    1 -> {
                        startGallery()
                    }
                    2 -> {
                        viewModel.setImageEmpty()
                    }
                }
            }
            .show()
    }

    private fun startCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                photoFile?.also {
                    val photoUri: Uri = FileProvider.getUriForFile(
                        this,
                        "com.kdjj.ratatouille.fileprovider",
                        it
                    ).also {
                        lastCameraFileUri = it.toString()
                    }
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw Exception()
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun startGallery() {
        galleryLauncher.launch(
            Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }
        )
    }
}