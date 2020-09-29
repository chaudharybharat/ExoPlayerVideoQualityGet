package com.bharat.myapplication

import android.media.MediaCodec
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer.*
import com.google.android.exoplayer.hls.*
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer.upstream.DefaultUriDataSource
import com.google.android.exoplayer.util.ManifestFetcher
import com.google.android.exoplayer.util.MimeTypes
import com.google.android.exoplayer.util.PlayerControl

import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity(),View.OnClickListener,ManifestFetcher.ManifestCallback<HlsPlaylist>,SurfaceHolder.Callback,ExoPlayer.Listener {

    var BUFFER_SUGMENT_SIZE=64*1024
    var MAIN_BUFFER_SUGMENT=256
    var player: ExoPlayer? = null
    var playerControl:PlayerControl? = null
    var mediaCodecAudioTrackRenderer:MediaCodecAudioTrackRenderer? = null
    var video_url:String? = null
    var handler:Handler? = null
    var playlistFetch:ManifestFetcher<HlsPlaylist>? = null
     var videorender: MediaCodecVideoTrackRenderer?=null
     var audiorender:MediaCodecAudioTrackRenderer?=null
    var tag="test"
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
       // video_url="http://playertest.longtailvideo.com/adaptive/bbbfull/bbbfull.m3u8"
       // video_url="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
      //  video_url="https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
        video_url="https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
        //video_url="http://demo.unified-streaming.com/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        var parser= HlsPlaylistParser()
        handler= Handler()
        playlistFetch=ManifestFetcher(video_url, DefaultUriDataSource(this, tag), parser)
        playlistFetch!!.singleLoad(handler!!.looper, this)
        surfaceView!!.holder.addCallback(this)
        btn_play.setOnClickListener(this)
        btn_pause.setOnClickListener(this)
        btn_Quality.setOnClickListener(this)
        val param = PlaybackParams()
        param.speed = 1f // 1f is 1x, 2f is 2x
        //player.setPlaybackParameters()

    }

    override fun onClick(v: View?) {
      when(v!!.id){
          R.id.btn_play -> {
              if (playerControl!!.isPlaying) {
                  playerControl!!.start()
                  //playerControl.start()
              }
          }
          R.id.btn_pause -> {
              if (playerControl!!.isPlaying) {
                  playerControl!!.pause()
                  //playerControl.start()
              }
          }
          R.id.btn_Quality -> {
              val popup = PopupMenu(this, v)
              popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                  override fun onMenuItemClick(item: MenuItem?): Boolean {
                      Log.e("test", "select postion" + (item!!.order))
                      Handler()!!.postDelayed(Runnable {
                          player!!.setSelectedTrack(0, item!!.order - 1)
                          var test = player!!.getTrackFormat(0, item!!.order - 1)
                          Log.e("test", "select truck media format" + test)


                      }, 10)

                      return false
                  }

              })
              val menu = popup.menu
              menu.add(Menu.NONE, 0, 0, "Video Quality")
              for (i in 0 until player!!.getTrackCount(0)) {
                  var mediaFormt = player!!.getTrackFormat(0, i);

                  if (MimeTypes.isVideo(mediaFormt.mimeType)) {
                      if (mediaFormt.adaptive) {
                          menu.add(1, (i + 1), (i + 1), "Auto")
                      } else {
                          menu.add(1, (i + 1), (i + 1), mediaFormt.width.toString() + "p")
                      }
                  }
              }
              menu.setGroupCheckable(1, true, true)
              menu.findItem((player!!.getSelectedTrack(0) + 1)).setChecked(true)

              popup.show()
          }
      }
    }

    override fun onSingleManifest(manifest: HlsPlaylist?) {
        player=ExoPlayer.Factory.newInstance(2)
        player!!.addListener(this)
        var loadControl =DefaultLoadControl(DefaultAllocator(BUFFER_SUGMENT_SIZE))
        var bandWithMetor=DefaultBandwidthMeter()
        var dataSource=DefaultUriDataSource(this, bandWithMetor, tag)
       var timestampAdjusterProvider=PtsTimestampAdjusterProvider()
       var chunkSource=HlsChunkSource(
           true, dataSource, manifest, DefaultHlsTrackSelector.newDefaultInstance(
               this
           ), bandWithMetor, timestampAdjusterProvider, HlsChunkSource.ADAPTIVE_MODE_SPLICE
       )
      var sampleSource= HlsSampleSource(
          chunkSource,
          loadControl,
          MAIN_BUFFER_SUGMENT * BUFFER_SUGMENT_SIZE
      )
       videorender=MediaCodecVideoTrackRenderer(
           this,
           sampleSource,
           MediaCodecSelector.DEFAULT,
           MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT
       )
        audiorender= MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT)

        player!!.prepare(videorender, audiorender)
      player!!.sendMessage(
          videorender,
          MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
          surfaceView.holder.surface
      )
        playerControl= PlayerControl(player)
        player!!.playWhenReady=true
    }

    override fun onSingleManifestError(e: IOException?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if(videorender!=null){
            player!!.blockingSendMessage(
                videorender,
                MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
                surfaceView
            )
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when(playbackState){
            ExoPlayer.STATE_READY -> {
                btn_Quality.isEnabled = true
            }
        }
    }

    override fun onPlayWhenReadyCommitted() {
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        player!!.release()
    }

}