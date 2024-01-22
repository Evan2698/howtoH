package com.copyland.howtoh

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.copyland.howtoh.databinding.FragmentMainBinding
import com.copyland.howtoh.service.ScreenMirrorService
import com.github.xfalcon.vhosts.vservice.VhostsService

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    companion object{
        private var TAG:String = MainFragment::class.java.simpleName
        private var LANDSCAPE_VALUE: Boolean = false
        private var buttonChecked:Boolean = false
    }

    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var appService :ScreenMirrorService? = null
    private var intent: Intent? = null
    private var requestScreenCapture: ActivityResultLauncher<Intent>? = null
    private var requestVpnLauncher: ActivityResultLauncher<Intent>? = null
    private var serviceConnection: AppServiceConnection? = null
    private var myCast :MyBroadcastReceiver? = null


    private inner class AppServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ScreenMirrorService.MirrorServiceBinder
            appService = binder.service
            if (appService != null){
                if (!appService!!.isServerRunning()){
                    activity?.runOnUiThread {
                        appService?.startService(intent!!, requireContext())
                        startVPN()
                    }
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.e(TAG, "Service unexpectedly exited")
            appService = null
        }
    }

    private inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent){
            var value:Int = intent.getIntExtra(ScreenMirrorService.SERVICE_STATUS, -1)
            when(value){
                1-> setButtonCheckStatus(true)
                2->setButtonCheckStatus(false)
                else->setButtonCheckStatus(false)
            }
        }
    }

    private fun registerBroadcast(){
        val filter = IntentFilter()
        filter.addAction(ScreenMirrorService.SERVICE_STATUS_ACTION)
        myCast = MyBroadcastReceiver()
        ContextCompat.registerReceiver(this.requireContext(), myCast!!, filter,ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterBroadcast(){
        this.requireContext().unregisterReceiver(myCast!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        registerLaunch()
        registerBroadcast()
        registerVPNLaunch()
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startButton.setOnClickListener {
            if (binding.startButton.isChecked){
                this.start()
            }else {
                this.stop()
            }
        }
        binding.startButton.setOnCheckedChangeListener { _, isChecked ->
            buttonChecked = isChecked
        }


        binding.landscapeId.setOnClickListener{
            LANDSCAPE_VALUE = true
        }
        binding.portraitId.setOnClickListener {
            LANDSCAPE_VALUE = false
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindService() {
        val serviceIntent = Intent(this.context, ScreenMirrorService::class.java)
        serviceConnection = AppServiceConnection()
        this.activity?.bindService(serviceIntent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (serviceConnection == null) return
        if (!ScreenMirrorService.IsServiceRunning) return
        this.activity?.unbindService(serviceConnection!!)
        serviceConnection = null
    }
    private fun startMediaService(){
        val serviceIntent = Intent(this.context, ScreenMirrorService::class.java)
        serviceIntent.action = ScreenMirrorService.SERVICE_START_ACTION
        ContextCompat.startForegroundService(this.requireContext(), serviceIntent)
        bindService()
    }
    private fun stopMediaService(){
        unbindService()
        var serviceIntent = Intent(this.context, ScreenMirrorService::class.java)
        ContextCompat.startForegroundService(this.requireContext(), serviceIntent)
        serviceIntent.action = ScreenMirrorService.SERVICE_STOP_ACTION
        this.activity?.startService(serviceIntent)

        serviceIntent = Intent(this.context, ScreenMirrorService::class.java)
        this.activity?.stopService(serviceIntent)
    }

    private fun requireMediaPermission(){
        val mediaProjectionManager =
            this.activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureIntent.let {
            requestScreenCapture?.launch(it)
        }
    }

    private fun registerLaunch(){
        requestScreenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleScreenMediaRouting(result)
        }
    }

    private fun handleScreenMediaRouting(result: ActivityResult){
        if (result.resultCode == RESULT_OK){
            var intent = result.data
            this.intent = intent
            startMediaService()
        }else {
            Log.d(TAG, "cancel the handleScreenMediaRouting!")
        }
    }

    override fun onResume() {
        super.onResume()
        binding.landscapeId.isChecked = LANDSCAPE_VALUE
        binding.portraitId.isChecked = !LANDSCAPE_VALUE
        if (appService == null && ScreenMirrorService.IsServiceRunning){
            bindService()
            Log.d("SM", "bindService")
        }

        setButtonCheckStatus(ScreenMirrorService.IsServiceRunning)
        binding.startButton.isChecked = buttonChecked

        Log.d("SM", "onResume ${ScreenMirrorService.IsServiceRunning}, $buttonChecked")
    }

    override fun onPause() {
        super.onPause()
        if (appService != null){
            unbindService()
            Log.d("SM", "unbindService")
        }
    }

    override fun onDestroy() {
        unregisterBroadcast()
        super.onDestroy()
    }


    private fun start(){
        requireMediaPermission()
    }
    private fun stop(){
        if (ScreenMirrorService.IsServiceRunning){
            stopMediaService()
        }
        shutdownVPN()
    }

    private fun setButtonCheckStatus(setFlag: Boolean) {
        if (setFlag) {
            binding.startButton.setBackgroundResource(R.drawable.bg_button_on)
        } else {
            binding.startButton.setBackgroundResource(R.drawable.bg_button_off)
        }
    }

    private fun startVPN() {
        val vpnIntent = VhostsService.prepare(this.requireContext())
        if (vpnIntent!= null){
            vpnIntent.let {
                requestVpnLauncher?.launch(it)
            }
        }else {
            startVPNService()
        }
    }

    private fun startVPNService(){
        this.activity?.startService(
            Intent(
                this.context,
                VhostsService::class.java
            ).setAction(VhostsService.ACTION_CONNECT)
        )
    }

    private fun shutdownVPN() {
        if (VhostsService.isRunning()) {
            this.activity?.startService(
                Intent(
                    this.context,
                    VhostsService::class.java
                ).setAction(VhostsService.ACTION_DISCONNECT)
            )
        }
    }
    private fun registerVPNLaunch(){
        requestVpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startVPNService()
            } else {
                Log.d(TAG, "cancel the projection!")
            }
        }
    }


}