package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.os.Bundle
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}