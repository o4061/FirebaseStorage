package com.example.firebasestorage

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebasestorage.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception


private const val REQUEST_CODE_IMAGE_PICK = 0
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var curFile: Uri? = null
    private val imageRef = Firebase.storage.reference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it, REQUEST_CODE_IMAGE_PICK)
            }
        }

        binding.btnUploadImage.setOnClickListener {
            uploadImageToStorage("myImage")
        }

        binding.btnDownloadImage.setOnClickListener {
            downloadImage("myImage")
        }

        binding.btnDeleteImage.setOnClickListener {
            deleteImage("myImage")
        }

        listFiles()
    }

    private fun listFiles(){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val images = imageRef.child("images/").listAll().await()
                val imageUrls = mutableListOf<String>()
                images.items.forEach {
                    val url = it.downloadUrl.await()
                    imageUrls.add(url.toString())
                }
                withContext(Dispatchers.Main){
                    val imageAdapter = ImageAdapter(imageUrls)
                    binding.rvImages.apply {
                        adapter = imageAdapter
                        layoutManager = LinearLayoutManager(this@MainActivity)
                    }
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteImage(fileName: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                imageRef.child("images/$fileName").delete().await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, "Successfully deleted image.", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadImage(fileName: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val maxDownloadSize = 5L * 1024 * 1024
                val bytes = imageRef.child("images/$fileName").getBytes(maxDownloadSize).await()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                withContext(Dispatchers.Main){
                    binding.ivImage.setImageBitmap(bmp)
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadImageToStorage(fileName: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                curFile?.let {
                    imageRef.child("images/$fileName").putFile(it).await()
                }
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, "Successfully uploaded image", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_IMAGE_PICK){
            data?.data?.let {
                curFile = it
                binding.ivImage.setImageURI(it)
            }
        }
    }
}