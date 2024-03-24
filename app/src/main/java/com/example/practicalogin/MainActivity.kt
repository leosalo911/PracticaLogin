package com.example.practicalogin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var oneTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        binding
        ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .build()
    }


    private fun showUser() {
        val user = Firebase.auth.currentUser
        user?.let {
            val name = it.displayName
            val email = it.email
            val photoUrl = it.photoUrl
            val emailVerified = it.isEmailVerified
            binding.txtName.text = name
            binding.txtEmail.text = email
            if (emailVerified) {
                binding.txtStatus.text = "Verified Email"
            }

            var image: Bitmap? = null
            val imageURL = photoUrl.toString()
            val executorService = Executors.newSingleThreadExecutor()
            executorService.execute {
                try {
                    val `in` = java.net.URL(imageURL).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            runOnUiThread {
                try {
                    Thread.sleep(1000)
                    binding.imageView.setImageBitmap(image)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) run {
            showUser()
        }
    }


    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ){ result ->
           if(result.resultCode == RESULT_OK){
               try{
                   val credential = oneTapClient!!.getSignInCredentialFromIntent(result.data)
                   val idToken = credential.googleIdToken
                   if(idToken != null){
                       val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                       auth.signInWithCredential(firebaseCredential).addOnCanceledListener {
                           if(it.isSuccesful){
                               binding.progressBar.visibility = View.INVISIBLE
                               Toast.makeText(this,"Sign in complete", Toast.LENGTH_LONG).show()
                               showUser()
                           }
                       }
                   }
               }catch (e: ApiException){
                   e.printStackTrace()
               }
           }

        }

    private suspend fun signingGoogle() {
        val result = oneTapClient?.beginSignIn(signInRequest)?.await()
        val intentSenderRequest = IntentSenderRequest.Builder(result!!.pendingIntent).build()
        activityResultLauncher.launch(intentSenderRequest)
    }


    fun signingGoogle(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            signingGoogle()
        }
    }


    fun signOutUser(view: View) {
        Firebase.auth.signOut()
        Toast.makeText(this, "Sign Out", Toast.LENGTH_LONG).show()
        binding.txtName.text = ""
        binding.txtEmail.text = ""
        binding.txtStatus.text = "Sign Out"
        binding.imageView.setImageBitmap(null)
    }


}
