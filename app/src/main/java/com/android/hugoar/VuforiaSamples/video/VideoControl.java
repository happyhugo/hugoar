package com.android.hugoar.VuforiaSamples.video;

import android.content.Intent;
import android.view.MotionEvent;

import com.android.hugoar.VuforiaSamples.app.ImageTargets.ImageTargets;

/**
 * Created by hugo on 16-5-13.
 */
public class VideoControl {
    ImageTargets activity;
    public static final int NUM_TARGETS = 2;
    public static final int STONES = 0;
    public static final int CHIPS = 1;
    public VideoPlayerHelper mVideoPlayerHelper[] = null;
    public int mSeekPosition[] = null;
    private boolean mWasPlaying[] = null;
    public String mMovieName[] = null;
    // A boolean to indicate whether we come from full screen:
    private boolean mReturningFromFullScreen = false;

    public VideoControl(ImageTargets activity){
        this.activity = activity;
        init();
    }

    public void init(){
        mVideoPlayerHelper = new VideoPlayerHelper[NUM_TARGETS];
        mSeekPosition = new int[NUM_TARGETS];
        mWasPlaying = new boolean[NUM_TARGETS];
        mMovieName = new String[NUM_TARGETS];

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = new VideoPlayerHelper();
            mVideoPlayerHelper[i].init();
            mVideoPlayerHelper[i].setActivity(activity);
        }

        mMovieName[STONES] = "VideoPlayback/VuforiaSizzleReel_1.mp4";
        mMovieName[CHIPS] = "VideoPlayback/VuforiaSizzleReel_2.mp4";
    }

    public void resumeVideo(){
        // Reload all the movies
        if (activity.mRenderer != null)
        {
            for (int i = 0; i < NUM_TARGETS; i++)
            {
                if (!mReturningFromFullScreen){
                    activity.mRenderer.videoModel.requestLoad(i, mMovieName[i], mSeekPosition[i],
                            false);
                } else{
                    activity.mRenderer.videoModel.requestLoad(i, mMovieName[i], mSeekPosition[i],
                            mWasPlaying[i]);
                }
            }
        }

        mReturningFromFullScreen = false;
    }

    public void backActivityResult(Intent data){
        // The following values are used to indicate the position in
        // which the video was being played and whether it was being
        // played or not:
        String movieBeingPlayed = data.getStringExtra("movieName");
        mReturningFromFullScreen = true;

        // Find the movie that was being played full screen
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            if (movieBeingPlayed.compareTo(mMovieName[i]) == 0)
            {
                mSeekPosition[i] = data.getIntExtra("currentSeekPosition", 0);
                mWasPlaying[i] = false;
            }
        }
    }

    public void pauseVideo(){
        // Store the playback state of the movies and unload them:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // If the activity is paused we need to store the position in which
            // this was currently playing:
            if (mVideoPlayerHelper[i].isPlayableOnTexture())
            {
                mSeekPosition[i] = mVideoPlayerHelper[i].getCurrentPosition();
                mWasPlaying[i] = mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING ? true
                        : false;
            }

            // We also need to release the resources used by the helper, though
            // we don't need to destroy it:
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].unload();
        }

        mReturningFromFullScreen = false;
    }

    public void destroyVideo(){
        for (int i = 0; i < NUM_TARGETS; i++) {
            // If the activity is destroyed we need to release all resources:
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].deinit();
            mVideoPlayerHelper[i] = null;
        }
    }


    // Pause all movies except one
    // if the value of 'except' is -1 then
    // do a blanket pause
    public void pauseAll(int except)
    {
        // And pause all the playing videos:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // We can make one exception to the pause all calls:
            if (i != except)
            {
                // Check if the video is playable on texture
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // If it is playing then we pause it
                    mVideoPlayerHelper[i].pause();
                }
            }
        }
    }

    public void setmVideoPlayerHelper(){
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            activity.mRenderer.videoModel.setVideoPlayerHelper(i, mVideoPlayerHelper[i]);
            activity.mRenderer.videoModel.requestLoad(i, mMovieName[i], 0, false);
        }
    }

    public void initApplicationAR(){
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            float[] temp = { 0f, 0f, 0f };
            activity.mRenderer.videoModel.targetPositiveDimensions[i].setData(temp);
            activity.mRenderer.videoModel.videoPlaybackTextureID[i] = -1;
        }
    }

    public boolean playOrPauseVideo(MotionEvent e){
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // Verify that the tap happened inside the target
            if (activity.mRenderer!= null && activity.mRenderer.videoModel.isTapOnScreenInsideTarget(i, e.getX(), e.getY()))
            {
                // Check if it is playable on texture
                System.out.println("aaaa:1");
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // We can play only if the movie was paused, ready
                    // or stopped
                    System.out.println("aaaa:2");
                    if ((mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                            || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.READY)
                            || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.STOPPED)
                            || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                    {
                        // Pause all other media
                        pauseAll(i);

                        // If it has reached the end then rewind
                        if ((mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                            mSeekPosition[i] = 0;
                        System.out.println("aaaa:3");
                        mVideoPlayerHelper[i].play(activity.mAppMenu.mPlayFullscreenVideo,
                                mSeekPosition[i]);
                        mSeekPosition[i] = VideoPlayerHelper.CURRENT_POSITION;
                    } else if (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
                    {
                        // If it is playing then we pause it
                        mVideoPlayerHelper[i].pause();
                    }
                } else if (mVideoPlayerHelper[i].isPlayableFullscreen())
                {
                    // If it isn't playable on texture
                    // Either because it wasn't requested or because it
                    // isn't supported then request playback fullscreen.
                    mVideoPlayerHelper[i].play(true,
                            VideoPlayerHelper.CURRENT_POSITION);
                }

                return true;

                // Even though multiple videos can be loaded only one
                // can be playing at any point in time. This break
                // prevents that, say, overlapping videos trigger
                // simultaneously playback.
            }
        }
        return false;
    }
}
