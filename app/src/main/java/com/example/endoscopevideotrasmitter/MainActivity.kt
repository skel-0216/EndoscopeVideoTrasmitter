package com.example.endoscopevideotrasmitter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.endoscopevideotrasmitter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.buttonActivityHotspot.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when(view){
            binding.buttonActivityHotspot->{
                val intent = Intent(this, HotspotActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
