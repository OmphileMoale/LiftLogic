package com.example.liftlogic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.liftlogic.ui.AppViewModel
import com.example.liftlogic.ui.LiftLogicNavGraph
import com.example.liftlogic.ui.theme.LiftLogicTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiftLogicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiftLogicNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}