package com.example.icymath.ui.helpcenter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.icymath.databinding.FragmentHelpCenterBinding

class HelpCenterFragment : Fragment() {

    private var _binding: FragmentHelpCenterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(HelpCenterViewModel::class.java)

        _binding = FragmentHelpCenterBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textViewTitle
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}