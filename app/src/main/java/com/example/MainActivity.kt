package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.database.AppDatabase
import com.example.data.repository.NutriRepository
import com.example.ui.screens.NutriTrackApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NutriViewModel
import com.example.ui.viewmodel.NutriViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NutriRepository(database.intakeDao())
        
        // ViewModel provisioning
        val viewModel: NutriViewModel by viewModels {
            NutriViewModelFactory(repository, applicationContext)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NutriTrackApp(viewModel = viewModel)
            }
        }
    }
}
