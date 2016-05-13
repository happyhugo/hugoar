package com.android.hugoar.VuforiaSamples.app.ImageTargets;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.Toast;

import com.android.hugoar.R;
import com.android.hugoar.SampleApplication.SampleApplicationException;
import com.android.hugoar.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.android.hugoar.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuInterface;
import com.android.hugoar.VuforiaSamples.video.VideoPlayerHelper;
import com.vuforia.CameraDevice;
import com.vuforia.Trackable;

/**
 * Created by hugo on 16-5-11.
 */
public class AppMenu implements SampleAppMenuInterface {

    private static final String LOGTAG = "AppMenu";

    final public static int CMD_BACK = -1;
    final public static int CMD_EXTENDED_TRACKING = 1;
    final public static int CMD_AUTOFOCUS = 2;
    final public static int CMD_FLASH = 3;
    final public static int CMD_CAMERA_FRONT = 4;
    final public static int CMD_CAMERA_REAR = 5;
    final public static int CMD_DATASET_START_INDEX = 6;
    final private static int CMD_FULLSCREEN_VIDEO = 7;

    ImageTargets activity;

    private boolean mFlash = false;
    private boolean mContAutofocus = false;
    private View mFlashOptionView;
    public GestureDetector mGestureDetector;
    public boolean mPlayFullscreenVideo = false;

    public AppMenu(Activity activity){
        this.activity = (ImageTargets)activity;
        mGestureDetector = new GestureDetector(activity, new GestureListener());
    }

    @Override
    public boolean menuProcess(int command)
    {

        boolean result = true;

        switch (command)
        {
            case CMD_BACK:
                activity.finish();
                break;

            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);

                if (result)
                {
                    mFlash = !mFlash;
                } else
                {
                    showToast(activity.getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                            activity.getString(mFlash ? R.string.menu_flash_error_off
                                    : R.string.menu_flash_error_on));
                }
                break;

            case CMD_AUTOFOCUS:

                if (mContAutofocus)
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result)
                    {
                        mContAutofocus = false;
                    } else
                    {
                        showToast(activity.getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG, activity.getString(R.string.menu_contAutofocus_error_off));
                    }
                } else
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result)
                    {
                        mContAutofocus = true;
                    } else
                    {
                        showToast(activity.getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,activity.getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

            case CMD_CAMERA_FRONT:
            case CMD_CAMERA_REAR:

                // Turn off the flash
                if (mFlashOptionView != null && mFlash)
                {
                    // OnCheckedChangeListener is called upon changing the checked state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                    {
                        ((Switch) mFlashOptionView).setChecked(false);
                    } else
                    {
                        ((CheckBox) mFlashOptionView).setChecked(false);
                    }
                }

                activity.vuforiaAppSession.stopCamera();

                try
                {
                    activity.vuforiaAppSession.startAR(command == CMD_CAMERA_FRONT ? CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_FRONT : CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_BACK);
                } catch (SampleApplicationException e)
                {
                    showToast(e.getString());
                    Log.e(LOGTAG, e.getString());
                    result = false;
                }
                activity.doStartTrackers();
                break;
            case CMD_FULLSCREEN_VIDEO:
                mPlayFullscreenVideo = !mPlayFullscreenVideo;

                for(int i = 0; i < activity.videoControl.mVideoPlayerHelper.length; i++)
                {
                    if (activity.videoControl.mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
                    {
                        // If it is playing then we pause it
                        activity.videoControl. mVideoPlayerHelper[i].pause();
                        activity.videoControl.mVideoPlayerHelper[i].play(true,activity.videoControl.mSeekPosition[i]);
                    }
                }
                break;
            case CMD_EXTENDED_TRACKING:
                for (int tIdx = 0; tIdx < activity.mCurrentDataset.getNumTrackables(); tIdx++)
                {
                    Trackable trackable = activity.mCurrentDataset.getTrackable(tIdx);

                    if (!activity.mExtendedTracking)
                    {
                        if (!trackable.startExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                    "Failed to start extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    } else
                    {
                        if (!trackable.stopExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                    "Failed to stop extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                    "Successfully started extended tracking target");
                        }
                    }
                }

                if (result)
                    activity.mExtendedTracking = !activity.mExtendedTracking;

                break;

            default:
                if (command >= activity.mStartDatasetsIndex
                        && command < activity.mStartDatasetsIndex + activity.mDatasetsNumber)
                {
                    activity.mSwitchDatasetAsap = true;
                    activity.mCurrentDatasetSelectionIndex = command - activity.mStartDatasetsIndex;
                }
                break;
        }

        return result;
    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        // Handle the single tap
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            return activity.videoControl.playOrPauseVideo(e);
        }

//        @Override
//        public boolean onSingleTapUp(MotionEvent e)
//        {
//            // Generates a Handler to trigger autofocus
//            // after 1 second
//            autofocusHandler.postDelayed(new Runnable()
//            {
//                public void run()
//                {
//                    boolean result = CameraDevice.getInstance().setFocusMode(
//                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
//
//                    if (!result)
//                        Log.e("SingleTapUp", "Unable to trigger focus");
//                }
//            }, 1000L);
//
//            return true;
//        }
    }

    // This method sets the menu's settings
    public void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;

        group = activity.mSampleAppMenu.addGroup("", false);
        group.addTextItem(activity.getString(R.string.menu_back), -1);

        group = activity.mSampleAppMenu.addGroup("", true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            group.addSelectionItem(activity.getString(R.string.menu_playFullscreenVideo),
                    CMD_FULLSCREEN_VIDEO, mPlayFullscreenVideo);
        }

        group = activity.mSampleAppMenu.addGroup("", true);
        group.addSelectionItem(activity.getString(R.string.menu_extended_tracking),
                CMD_EXTENDED_TRACKING, false);
        group.addSelectionItem(activity.getString(R.string.menu_contAutofocus),
                CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(activity.getString(R.string.menu_flash), CMD_FLASH, false);

        Camera.CameraInfo ci = new Camera.CameraInfo();
        boolean deviceHasFrontCamera = false;
        boolean deviceHasBackCamera = false;
        for (int i = 0; i < Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                deviceHasFrontCamera = true;
            else if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                deviceHasBackCamera = true;
        }

        if (deviceHasBackCamera && deviceHasFrontCamera)
        {
            group = activity.mSampleAppMenu.addGroup(activity.getString(R.string.menu_camera),
                    true);
            group.addRadioItem(activity.getString(R.string.menu_camera_front),
                    CMD_CAMERA_FRONT, false);
            group.addRadioItem(activity.getString(R.string.menu_camera_back),
                    CMD_CAMERA_REAR, true);
        }

        group = activity.mSampleAppMenu
                .addGroup(activity.getString(R.string.menu_datasets), true);
        activity.mStartDatasetsIndex = CMD_DATASET_START_INDEX;
        activity.mDatasetsNumber = activity.mDatasetStrings.size();

        group.addRadioItem("Stones & Chips", activity.mStartDatasetsIndex, true);
        group.addRadioItem("Tarmac", activity.mStartDatasetsIndex + 1, false);

        activity.mSampleAppMenu.attachMenu();
    }

    public void showToast(String text)
    {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
    }

    public void turnoffFlash(){
        if (mFlashOptionView != null && mFlash)
        {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                ((Switch) mFlashOptionView).setChecked(false);
            } else
            {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }
    }

    public void setAutoFocus(boolean autoFocus){
        mContAutofocus = autoFocus;
    }
}
