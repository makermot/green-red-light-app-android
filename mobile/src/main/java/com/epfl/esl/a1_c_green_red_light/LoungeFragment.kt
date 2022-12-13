package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoungeBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.Wearable

class LoungeFragment : Fragment() {

    private lateinit var binding: FragmentLoungeBinding
    private lateinit var viewModel: SharedViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lounge, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        return inflater.inflate(R.layout.fragment_lounge, container, false)
    }

    private fun sendCommandToWear(command: String){ Thread(Runnable {
        val connectedNodes: List<String> = Tasks
            .await(Wearable
                .getNodeClient(activity as MainActivity).connectedNodes)
            .map { it.id }
        connectedNodes.forEach {
            val messageClient: MessageClient = Wearable
                .getMessageClient(activity as AppCompatActivity)
            messageClient.sendMessage(it, "/command", command.toByteArray())
        } }).start()
    }

}