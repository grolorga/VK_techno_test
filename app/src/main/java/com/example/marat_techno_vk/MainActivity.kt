package com.example.marat_techno_vk

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.mediarouter.R
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : FragmentActivity() {
    private lateinit var castContext: CastContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        castContext = CastContext.getSharedInstance(this@MainActivity)
        lifecycleScope.launch(Dispatchers.Main) {


            val mediaRouteSelector = castContext.mergedSelector
            if (mediaRouteSelector != null) {
                setContent {
                    CastAppUI(castContext)
                }
            } else {
                Log.e("CastApp", "Ошибка: mediaRouteSelector == null")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        castContext.sessionManager.addSessionManagerListener(sessionManagerListener, Session::class.java)
    }

    override fun onStop() {
        super.onStop()
        castContext.sessionManager.removeSessionManagerListener(sessionManagerListener, Session::class.java)
    }

    private val sessionManagerListener = object : SessionManagerListener<Session> {
        override fun onSessionStarted(session: Session, sessionId: String) {
            Log.d("CastDebug", "Сессия Cast началась: $sessionId")
            playVideo()
        }

        override fun onSessionResumed(session: Session, wasSuspended: Boolean) {
            Log.d("CastDebug", "Сессия Cast возобновлена")
            playVideo()
        }

        override fun onSessionEnded(session: Session, error: Int) {
            Log.d("CastDebug", "Сессия Cast завершена")
        }

        override fun onSessionStarting(session: Session) {}
        override fun onSessionResuming(session: Session, sessionId: String) {}
        override fun onSessionSuspended(session: Session, reason: Int) {}
        override fun onSessionEnding(session: Session) {}
        override fun onSessionResumeFailed(p0: Session, p1: Int) {}
        override fun onSessionStartFailed(p0: Session, p1: Int) {}
    }

    private fun playVideo() {
        val session = castContext.sessionManager.currentCastSession
        if (session == null || !session.isConnected) {
            Log.e("CastDebug", "Нет активной сессии для воспроизведения")
            return
        }

        val remoteMediaClient: RemoteMediaClient? = session.remoteMediaClient
        if (remoteMediaClient == null) {
            Log.e("CastDebug", "remoteMediaClient == null")
            return
        }

        Log.d("CastDebug", "Запуск видео на Cast")

        val mediaInfo = MediaInfo.Builder("https://videolink-test.mycdn.me/?pct=1&sig=6QNOvp0y3BE&ct=0&clientType=45&mid=193241622673&type=5")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .build()

        val requestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .build()

        remoteMediaClient.load(requestData).setResultCallback { result ->
            if (!result.status.isSuccess) {
                Log.e("CastDebug", "Ошибка загрузки видео: ${result.status.statusMessage}")
            } else {
                Log.d("CastDebug", "Видео успешно загружено в Chromecast")
            }
        }
    }
}


@SuppressLint("PrivateResource")
@Composable
fun CastAppUI(castContext: CastContext) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center){
        AndroidView(
            modifier = Modifier.size(48.dp),
            factory = { context ->
                val themedContext = ContextThemeWrapper(context, R.style.Theme_MediaRouter)
                MediaRouteButton(themedContext).apply {
                    val mediaRouteSelector = castContext.mergedSelector
                    if (mediaRouteSelector != null) {
                        routeSelector = mediaRouteSelector
                    }
                    setBackgroundColor(R.style.Theme_MediaRouter)
                }
            }
        )
    }

}
