package com.example.farmerscollective.realtime

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmerscollective.R
import com.example.farmerscollective.databinding.FragmentOdkBinding
import com.example.farmerscollective.utils.Utils.Companion.traders
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class OdkFragment : Fragment() {

    private val viewModel by activityViewModels<OdkViewModel>()
    private lateinit var binding: FragmentOdkBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_odk, container, false)

        with(binding) {

            val spinAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.odk_filter))
            spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            filterSpinner.adapter = spinAdapter
            filterSpinner.setSelection(viewModel.filter.value!!)

            filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.filter(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //pass
                }
            }

            val viewAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.view))
            viewAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            viewSpinner.adapter = viewAdapter
            viewSpinner.setSelection(viewModel.view.value!!)

            viewSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.view(p2)
                    if(p2 == 1) {
                        list.visibility = View.GONE

                        barChart.visibility = View.VISIBLE
                        button.visibility = View.GONE
                    }
                    else if(p2 == 0) {
                        list.visibility = View.VISIBLE

                        barChart.visibility = View.GONE
                        button.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //pass
                }
            }

            val cropNameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.cropName))
            cropNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            cropSpinner.adapter = cropNameAdapter
            cropSpinner.setSelection(viewModel.crop.value!!)

            cropSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.chooseCrop(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //pass
                }
            }

            viewModel.list.observe(viewLifecycleOwner) {
                Log.d(this.toString(), it.keys.toString())
                val adapter = OdkAdapter(it.keys.toTypedArray(), object: OdkAdapter.Listener {
                    override fun onClick(date: LocalDate) {
                        viewModel.selectSubmission(date)

                        viewSpinner.visibility = View.GONE
                        filterSpinner.visibility = View.GONE
                        cropSpinner.visibility = View.GONE
                        list.visibility = View.GONE

                        barChart.visibility = View.VISIBLE
                        button.visibility = View.VISIBLE
                    }
                })

                list.adapter = adapter
                list.layoutManager = LinearLayoutManager(requireContext())

                if(viewModel.view.value == 0) {
                    val list = viewModel.dateSelect.value
                    val entries: ArrayList<BarEntry> = ArrayList()
                    val axis = ArrayList<String>()

                    if (list != null) {
                        for(i in list) {
                            if(i!!.localTraderId!! != -1) axis.add(traders[i.localTraderId!! - 1]) else axis.add("Not filled")
                        }
                    }

                    Log.d(this.toString(), axis.toString())

                    //fit the data into a bar

                    //fit the data into a bar
                    if (list != null) {
                        for (i in 0 until list.size) {
                            val barEntry = BarEntry(i.toFloat(), list.get(i)!!.price.toFloat())
                            entries.add(barEntry)
                        }
                    }

                    val barDataSet = BarDataSet(entries, "")

                    val data = BarData(barDataSet)
                    barChart.axisRight.isEnabled = false
                    barChart.data = data
                    barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    barChart.xAxis.granularity = 1f
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(axis)
                    barChart.isHighlightPerDragEnabled = false
                    barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener
                    {
                        override fun onValueSelected(e: Entry, h: Highlight?) {
                            val x = e.x.toString()
//                        val y = e.y.toString()
                            val selectedXAxisCount = x.substringBefore(".")
                            val dataDialogBuilder: AlertDialog.Builder? = activity?.let { fragmentActivity ->
                                AlertDialog.Builder(fragmentActivity)
                            }
                            val selectedOdkSubmission = list?.get(selectedXAxisCount.toInt())
                            val traderName = if(selectedOdkSubmission?.localTraderId!! == -1) "Not filled" else traders[selectedOdkSubmission.localTraderId - 1]
                            val mandalId = if(selectedOdkSubmission.mandalId == "") "Not filled" else selectedOdkSubmission.mandalId
                            dataDialogBuilder?.setMessage("Trader Name: ${traderName}\nMandal: ${mandalId}\nPrice: Rs ${selectedOdkSubmission.price}\nFilled by: ${selectedOdkSubmission.personFillingId}\nFilled on: ${selectedOdkSubmission.date}")!!
                                .setCancelable(false)
                                .setPositiveButton("Dismiss") { dialog, _ ->
                                    barChart.highlightValues(null)
                                    dialog.dismiss()
                                }
                            val dataDialog: AlertDialog = dataDialogBuilder.create()
                            dataDialog.setTitle("ODK Data")
                            dataDialog.show()
                        }

                        override fun onNothingSelected() {
//                        pass
                        }
                    })
                    barChart.invalidate()
                }
                else {
                    viewSpinner.setSelection(viewModel.view.value!!)
                    val list = viewModel.list.value
                    val entries: ArrayList<BarEntry> = ArrayList()
                    val axis = ArrayList<String?>()

                    if (list != null) {
                        for(i in list) {
                            val formatter: DateTimeFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val count = i.value.size
                            if(count % 2 == 1) {
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                                axis.add(i.key.format(formatter))
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                            }
                            else {
                                for (j in 0 until (count / 2) - 1) {
                                    axis.add("          ")
                                }
                                axis.add(i.key.format(formatter))
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                            }
                            axis.add("          ")
                        }
                    }
                    Log.e("AXIS", axis.toString())

                    if (list != null) {
                        var count = 0
                        for (i in list) {
                            var pos = count.toFloat()
                            for(j in i.value) {
                                val barEntry = BarEntry(pos, j!!.price.toFloat())
                                entries.add(barEntry)
                                pos += 1
                            }
                            count += i.value.size + 1
                        }
                    }
                    Log.e("TAG", entries.toString())

                    val barDataSet = BarDataSet(entries, "")

                    val data = BarData(barDataSet)
                    barChart.axisRight.isEnabled = false
                    barChart.data = data
                    barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    barChart.xAxis.granularity = 1f
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(axis)
                    barChart.isHighlightPerDragEnabled = false
                    barChart.setOnChartValueSelectedListener(null)
                    barChart.invalidate()
                }

            }

            viewModel.dateSelect.observe(viewLifecycleOwner) {

                val valueList = it
                val entries: ArrayList<BarEntry> = ArrayList()
                val axis = ArrayList<String>()

                for(i in it) {
                    if(i!!.localTraderId!! != -1) axis.add(traders[i.localTraderId!! - 1]) else axis.add("Not filled")
                }

                Log.d(this.toString(), axis.toString())

                //fit the data into a bar

                //fit the data into a bar
                for (i in 0 until valueList.size) {
                    val barEntry = BarEntry(i.toFloat(), valueList[i]!!.price.toFloat())
                    entries.add(barEntry)
                }

                val barDataSet = BarDataSet(entries, "")

                val data = BarData(barDataSet)
                barChart.axisRight.isEnabled = false
                barChart.data = data
                barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                barChart.xAxis.granularity = 1f
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(axis)
                barChart.isHighlightPerDragEnabled = false
                barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener
                {
                    override fun onValueSelected(e: Entry, h: Highlight?) {
                        val x = e.x.toString()
//                        val y = e.y.toString()
                        val selectedXAxisCount = x.substringBefore(".")
                        val dataDialogBuilder: AlertDialog.Builder? = activity?.let { fragmentActivity ->
                            AlertDialog.Builder(fragmentActivity)
                        }
                        val selectedOdkSubmission = it[selectedXAxisCount.toInt()]
                        val traderName = if(selectedOdkSubmission?.localTraderId!! == -1) "Not filled" else traders[selectedOdkSubmission.localTraderId - 1]
                        val mandalId = if(selectedOdkSubmission.mandalId == "") "Not filled" else selectedOdkSubmission.mandalId
                        dataDialogBuilder?.setMessage("Trader Name: ${traderName}\nMandal: ${mandalId}\nPrice: Rs ${selectedOdkSubmission.price}\nFilled by: ${selectedOdkSubmission.personFillingId}\nFilled on: ${selectedOdkSubmission.date}")!!
                            .setCancelable(false)
                            .setPositiveButton("Dismiss") { dialog, _ ->
                                barChart.highlightValues(null)
                                dialog.dismiss()
                            }
                        val dataDialog: AlertDialog = dataDialogBuilder.create()
                        dataDialog.setTitle("ODK Data")
                        dataDialog.show()
                    }

                    override fun onNothingSelected() {
//                        pass
                    }
                })
                barChart.invalidate()
            }

            viewModel.view.observe(viewLifecycleOwner) {
                if(it == 1) {
                    viewSpinner.setSelection(viewModel.view.value!!)
                    val list = viewModel.list.value
                    val entries: ArrayList<BarEntry> = ArrayList()
                    val axis = ArrayList<String?>()

                    if (list != null) {
                        for(i in list) {
                            val formatter: DateTimeFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val count = i.value.size
                            if(count % 2 == 1) {
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                                axis.add(i.key.format(formatter))
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                            }
                            else {
                                for (j in 0 until (count / 2) - 1) {
                                    axis.add("          ")
                                }
                                axis.add(i.key.format(formatter))
                                for (j in 0 until count / 2) {
                                    axis.add("          ")
                                }
                            }
                            axis.add("          ")
                        }
                    }
                    Log.e("AXIS", axis.toString())

                    if (list != null) {
                        var count = 0
                        for (i in list) {
                            var pos = count.toFloat()
                            for(j in i.value) {
                                val barEntry = BarEntry(pos, j!!.price.toFloat())
                                entries.add(barEntry)
                                pos += 1
                            }
                            count += i.value.size + 1
                        }
                    }
                    Log.e("TAG", entries.toString())

                    val barDataSet = BarDataSet(entries, "")

                    val data = BarData(barDataSet)
                    barChart.axisRight.isEnabled = false
                    barChart.data = data
                    barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    barChart.xAxis.granularity = 1f
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(axis)
                    barChart.isHighlightPerDragEnabled = false
                    barChart.setOnChartValueSelectedListener(null)
                    barChart.invalidate()
                }
                else {
                    viewSpinner.setSelection(viewModel.view.value!!)
                }
            }

            button.setOnClickListener {
                viewSpinner.visibility = View.VISIBLE
                filterSpinner.visibility = View.VISIBLE
                cropSpinner.visibility = View.VISIBLE
                list.visibility = View.VISIBLE

                barChart.visibility = View.GONE
                button.visibility = View.GONE

            }

        }

        return binding.root
    }

}