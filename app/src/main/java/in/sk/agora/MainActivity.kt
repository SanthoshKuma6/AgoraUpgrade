package `in`.sk.agora

/**
 * Created by santhosh on 20/12/2023.
 */
import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import `in`.santhosh.agora.R
import `in`.santhosh.agora.databinding.ActivityMainBinding
import `in`.santhosh.agora.databinding.LayoutBinding
import `in`.sk.agora.agora.media.RtcTokenBuilder
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {
    private var callingLayoutBinding:LayoutBinding?=null
    private var callDialog: Dialog? = null
    private val appId = "9aee2957b3084f8a8b3066210514dbe9"
    private var appCertificate = "76be346b4d9f4a14bde21e4a0b43bdbc"
    private var expirationTimeInSeconds = 3600
    private val channelName = "welcome Admin"
    //    private var token : String? = null
    private var token =
        "007eJxTYKhUatjyXblz2f5c2yNHBCdJBzs2Knk8iV2ZK73qtu564TUKDJaJqalGlqbmScYGFiZpFokWQIaZmZGhgamhSUpSquXvtU2pDYGMDAc0H7AwMkAgiM/LEJyYV5KRX5yRXZqbWMTAAABPUiL3 "
    private val uid = 0
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }


    private val mainActivity by lazy { ActivityMainBinding.inflate(layoutInflater) }
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainActivity.root)
        callingLayoutBinding=LayoutBinding.inflate(layoutInflater)
        val videoCallIcon: ImageView = findViewById(R.id.JoinButton)
        val animatorSet = AnimatorInflater.loadAnimator(this, R.anim.pulsating) as AnimatorSet
        animatorSet.setTarget(videoCallIcon)
        animatorSet.start()

        showCallInvitation()
        mainActivity.JoinButton.setOnClickListener {
            callDialog?.show()
            mainActivity.JoinButton.visibility=View.GONE
            mainActivity.LeaveButton.visibility=View.GONE
            mainActivity.audio.visibility=View.GONE
            mainActivity.video.visibility=View.GONE
            callingLayoutBinding!!.acceptBtn.setOnClickListener {
                joinChannel()
                callDialog!!.dismiss()
                mainActivity.JoinButton.visibility=View.GONE
                mainActivity.LeaveButton.visibility=View.VISIBLE
                mainActivity.audio.visibility=View.VISIBLE
                mainActivity.video.visibility=View.VISIBLE


            }
            callingLayoutBinding!!.rejectBtn.setOnClickListener {
                callDialog!!.dismiss()
                mainActivity.JoinButton.visibility=View.VISIBLE
                mainActivity.LeaveButton.visibility=View.VISIBLE
            }
        }
        mainActivity.LeaveButton.setOnClickListener { leaveChannel() }


        /**
         * below functionality is only for getting Result from Java Class
         */
        val tokenBuilder = RtcTokenBuilder()
        val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
        val result = tokenBuilder.buildTokenWithUid(
            appId, appCertificate,
            channelName, uid, RtcTokenBuilder.Role.ROLE_PUBLISHER, timestamp, timestamp
        )
        println(result)
        token = result
        Log.d("result", "onCreate: $result")
        /**
         *  above functionality is only for getting Result from Java Class
         */

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        setupVideoSDKEngine()

    }

    private fun showCallInvitation() {
        callDialog = Dialog(this, R.style.Theme_Dialog)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(callDialog!!.window!!.attributes)
        callDialog!!.setCanceledOnTouchOutside(true)
        callDialog!!.window!!.setBackgroundDrawableResource(R.color.trans)
        lp.windowAnimations
        callDialog!!.window!!.attributes = lp
        callDialog!!.setContentView(callingLayoutBinding!!.root)
        callDialog!!.create()
    }


    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")

            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")

        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        mainActivity.remoteVideoViewContainer.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        mainActivity.localVideoViewContainer.addView(localSurfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun joinChannel() {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, options)
            mainActivity.JoinButton.visibility=View.GONE
            mainActivity.LeaveButton.visibility=View.VISIBLE
        } else {
            Toast.makeText(applicationContext, "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun onJoinChannelClicked() {
        agoraEngine?.joinChannel(
            null,
            "test-channel",
            "Extra Optional Data",
            0
        ) // if you do not specify the uid, Agora will assign one.
        setupLocalVideoFeed()
        mainActivity.JoinButton.visibility=View.GONE
        mainActivity.LeaveButton.visibility=View.VISIBLE

    }

    private fun setupLocalVideoFeed() {
        // setup the container for the local user
        val videoContainer: FrameLayout = findViewById(R.id.remote_video_view_container)
        val videoSurface = RtcEngine.CreateRendererView(baseContext)
        videoSurface.setZOrderMediaOverlay(true)
        videoContainer.addView(videoSurface)
        agoraEngine?.setupLocalVideo(VideoCanvas(videoSurface, VideoCanvas.RENDER_MODE_FIT, 0))
    }


    private fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
            mainActivity.JoinButton.visibility=View.VISIBLE
            mainActivity.audio.visibility=View.GONE
            mainActivity.video.visibility=View.GONE
        }
    }

    fun onVideoMuteClicked(view:View) {
        val btn = view as ImageView
        if (btn.isSelected) {
            btn.isSelected = false
            Toast.makeText(this,"camera On",Toast.LENGTH_SHORT).show()
            btn.setImageResource(R.drawable.video_toggle_active_btn)
//            mainActivity.videoInactive.visibility=View.VISIBLE
//            mainActivity.video.visibility=View.GONE
        } else {
            btn.isSelected = true
            btn.setImageResource(R.drawable.video_toggle_inactive_btn)
            Toast.makeText(this,"camera Off",Toast.LENGTH_SHORT).show()

//            mainActivity.videoInactive.visibility=View.GONE
//            mainActivity.video.visibility=View.VISIBLE
        }

        agoraEngine!!.muteLocalVideoStream(btn.isSelected)
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        container.visibility = if (btn.isSelected) View.GONE else View.VISIBLE

        val videoSurface = container.getChildAt(0) as? SurfaceView
        videoSurface?.setZOrderMediaOverlay(!btn.isSelected)
        videoSurface?.visibility = if (btn.isSelected) View.GONE else View.VISIBLE
    }


    fun onAudioMuteClicked(view: View) {
        val btn = view as ImageView
        if (btn.isSelected) {
            btn.isSelected = false
            btn.setImageResource(R.drawable.audio_toggle_active_btn)
            Toast.makeText(this,"muted on",Toast.LENGTH_SHORT).show()
//            mainActivity.audio.visibility=View.VISIBLE
//            mainActivity.inactiveAudio.visibility=View.GONE
        } else {
            btn.isSelected = true
            btn.setImageResource(R.drawable.audio_toggle_inactive_btn)
            Toast.makeText(this,"muted off",Toast.LENGTH_SHORT).show()
//            mainActivity.audio.visibility=View.GONE
//            mainActivity.inactiveAudio.visibility=View.VISIBLE
        }
        agoraEngine!!.muteLocalAudioStream(btn.isSelected)
    }


}