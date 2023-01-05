package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentStatBinding
import com.google.firebase.database.*

class StatFragment : Fragment() {
    private lateinit var viewModelStat: StatViewModel
    private lateinit var viewModelShared: SharedViewModel

    var username: String = "alo"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentStatBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_stat, container, false
        )

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : My statistics"

        // Set the LayoutManager that this RecyclerView will use.
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        //binding.recyclerViewItems.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // Instantiate the viewModels
        viewModelStat = ViewModelProvider(this).get(StatViewModel::class.java)
        viewModelShared = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Retrieve current user
        username = viewModelShared.key
        print("USer in frag :")
        println(username)

        // Retrieve stat for that user
        viewModelStat.listenForStat(context,username)

        viewModelStat.statUpdate.observe(viewLifecycleOwner, Observer { statUpdate ->
            if(statUpdate == true){
                binding.recyclerViewItems.adapter = viewModelStat.itemAdapter
                viewModelStat.resetUpdate()
            }
        })
        return binding.root
    }
}